"""Host-side preparation and fail-closed execution of the Fiji integration test."""

import argparse
import json
import os
from pathlib import Path
import re
import shutil
import signal
import stat
import subprocess
import threading
import xml.etree.ElementTree as ET
import zipfile


WORKSPACE = Path(__file__).resolve().parents[2]
MAVEN_NS = "http://maven.apache.org/POM/4.0.0"


def project_info(pom):
    root = ET.parse(pom).getroot()
    ns = {"m": MAVEN_NS}
    return {key: root.findtext("m:" + key, namespaces=ns)
            for key in ("groupId", "artifactId", "version")}


def write_json(path, value):
    path.write_text(json.dumps(value, indent=2) + "\n", encoding="utf-8")


def configure(workspace=WORKSPACE):
    pom = workspace / "pom.xml"
    jdll = project_info(workspace / "ci-sources/jdll/pom.xml")
    tree = ET.parse(pom)
    root = tree.getroot()
    ns = {"m": MAVEN_NS}
    samj_version = root.findtext("m:properties/m:samj.version", namespaces=ns)
    # Dependency management overrides the JDLL version in SAMJ's published POM.
    management = root.find("m:dependencyManagement", ns)
    if management is None:
        management = ET.SubElement(root, "{%s}dependencyManagement" % MAVEN_NS)
    dependencies = management.find("m:dependencies", ns)
    if dependencies is None:
        dependencies = ET.SubElement(management, "{%s}dependencies" % MAVEN_NS)
    dependency = next((dep for dep in dependencies
                       if dep.findtext("m:groupId", namespaces=ns) == jdll["groupId"]
                       and dep.findtext("m:artifactId", namespaces=ns) == jdll["artifactId"]), None)
    if dependency is None:
        dependency = ET.SubElement(dependencies, "{%s}dependency" % MAVEN_NS)
    for key, value in jdll.items():
        node = dependency.find("m:" + key, ns)
        if node is None:
            node = ET.SubElement(dependency, "{%s}%s" % (MAVEN_NS, key))
        node.text = value
    ET.register_namespace("", MAVEN_NS)
    tree.write(pom, encoding="utf-8", xml_declaration=True)
    print("Using published SAMJ %s and source-built JDLL %s" % (samj_version, jdll["version"]))


def bundle(workspace=WORKSPACE):
    root = workspace / "ci-bundle"
    artifacts = []
    for jar in sorted((root / "jars").rglob("*.jar")):
        parts = jar.relative_to(root / "jars").parts
        artifacts.append({"groupId": ".".join(parts[:-3]), "artifactId": parts[-3],
                          "version": parts[-2], "path": jar.relative_to(root).as_posix(),
                          "destination": "jars"})
    plugin, = (root / "plugin").glob("*.jar")
    artifacts.append(dict(project_info(workspace / "pom.xml"),
                          path=plugin.relative_to(root).as_posix(),
                          destination="plugins"))
    samj, = [a for a in artifacts if a["groupId"] == "ai.nets" and a["artifactId"] == "samj"]
    sources = {"samj": {key: samj[key] for key in ("groupId", "artifactId", "version")}}
    sources["samj"]["origin"] = "Maven repository"
    checkout = workspace / "ci-sources/jdll"
    sources["jdll"] = dict(project_info(checkout / "pom.xml"), commit=subprocess.check_output(
        ["git", "-C", str(checkout), "rev-parse", "HEAD"], text=True).strip())
    jdll, = [a for a in artifacts if a["artifactId"] == sources["jdll"]["artifactId"]]
    if jdll["version"] != sources["jdll"]["version"]:
        raise RuntimeError("Resolved JDLL version differs from source build")
    built_jar = checkout / "target" / (jdll["artifactId"] + "-" + jdll["version"] + ".jar")
    shutil.copy2(built_jar, root / jdll["path"])
    sources["plugin"] = {"commit": subprocess.check_output(
        ["git", "-C", str(workspace), "rev-parse", "HEAD"], text=True).strip()}
    write_json(root / "manifest.json", {"sources": sources, "artifacts": artifacts})


def extract_archive(archive, destination):
    destination.mkdir(parents=True, exist_ok=False)
    with zipfile.ZipFile(archive) as zipped:
        for entry in zipped.infolist():
            target = destination / entry.filename
            if not target.resolve().is_relative_to(destination.resolve()):
                raise ValueError("Archive path escapes extraction directory")
            mode = entry.external_attr >> 16
            if stat.S_ISLNK(mode):
                link = zipped.read(entry).decode("utf-8")
                if not (target.parent / link).resolve().is_relative_to(destination.resolve()):
                    raise ValueError("Archive symlink escapes extraction directory")
                target.parent.mkdir(parents=True, exist_ok=True)
                target.symlink_to(link)
            else:
                zipped.extract(entry, destination)
                if os.name != "nt" and mode & 0o777:
                    target.chmod(mode & 0o777)


def install_jars(root, bundle_root, manifest):
    artifacts = manifest["artifacts"]
    for artifact in artifacts:
        source = bundle_root / artifact["path"]
        if not source.is_file():
            raise FileNotFoundError(source)
    patterns = [re.compile(r"^" + re.escape(a["artifactId"]) + r"(?:-[0-9].*)?\.jar$", re.I)
                for a in artifacts]
    for folder in (root / "jars", root / "plugins"):
        for existing in folder.rglob("*.jar"):
            if any(pattern.match(existing.name) for pattern in patterns):
                print("Replacing " + str(existing), flush=True)
                existing.unlink()
    for artifact in artifacts:
        source = bundle_root / artifact["path"]
        destination = root / artifact["destination"] / source.name
        shutil.copy2(source, destination)


def prepare(workspace=WORKSPACE):
    output = workspace / "test-output"
    output.mkdir(exist_ok=True)
    archive = workspace / "fiji.zip"
    extract_archive(archive, workspace / "fiji")
    launcher, = (workspace / "fiji").rglob(os.environ["FIJI_LAUNCHER"])
    root = next(parent for parent in launcher.parents
                if (parent / "jars").is_dir() and (parent / "plugins").is_dir())
    if os.name != "nt":
        launcher.chmod(launcher.stat().st_mode | stat.S_IXUSR)
    manifest = json.loads((workspace / "ci-bundle/manifest.json").read_text())
    install_jars(root, workspace / "ci-bundle", manifest)
    write_json(output / "build-manifest.json", manifest)
    write_json(output / "fiji.json", {"launcher": str(launcher), "root": str(root)})


def stop_process_tree(process):
    if os.name == "nt":
        subprocess.run(["taskkill", "/PID", str(process.pid), "/T", "/F"], check=False)
    else:
        try:
            os.killpg(process.pid, signal.SIGKILL)
        except ProcessLookupError:
            pass
    process.wait(timeout=30)


def launch(command, cwd, env, log_path, timeout):
    with log_path.open("w", encoding="utf-8") as log:
        process = subprocess.Popen(command, cwd=cwd, env=env, stdout=subprocess.PIPE,
                                   stderr=subprocess.STDOUT, text=True, encoding="utf-8",
                                   errors="replace", start_new_session=os.name != "nt")

        def forward_output():
            with process.stdout as stream:
                for line in stream:
                    print(line, end="", flush=True)
                    log.write(line)
                    log.flush()

        reader = threading.Thread(target=forward_output, daemon=True)
        reader.start()
        try:
            code = process.wait(timeout=timeout)
        except subprocess.TimeoutExpired:
            stop_process_tree(process)
            raise RuntimeError("Fiji timed out; see fiji.log")
        finally:
            reader.join(timeout=30)
        if code:
            raise RuntimeError("Fiji exited with code " + str(code))


def verify_results(output):
    installed = json.loads((output / "installation.json").read_text())
    result = json.loads((output / "inference.json").read_text())
    if not installed.get("installed") or result.get("foreground_pixels", 0) <= 0:
        raise RuntimeError("SAM2 Tiny installation or inference failed")
    mask = output / "mask.tif"
    if not mask.is_file() or mask.stat().st_size == 0:
        raise RuntimeError("Missing exported mask")
    print(json.dumps(result, indent=2))


def run(workspace=WORKSPACE):
    output = workspace / "test-output"
    fiji = json.loads((output / "fiji.json").read_text())
    model_dir = Path(fiji["root"]) / "sam2-test-env"
    if model_dir.exists():
        raise RuntimeError("Test requires a fresh SAM2 environment")
    for name in ("installation.json", "inference.json", "mask.tif"):
        (output / name).unlink(missing_ok=True)
    env = dict(os.environ, SAMJ_TEST_MODEL_DIR=str(model_dir),
               SAMJ_TEST_OUTPUT_DIR=str(output), PYTHONUNBUFFERED="1")
    command = [fiji["launcher"], "--headless", "--console", "--no-python", "--run",
               str(workspace / ".github/scripts/test_sam2.py")]
    print("Launching: " + repr(command), flush=True)
    launch(command, fiji["root"], env, output / "fiji.log", timeout=3600)
    # Fiji may report script errors without returning a nonzero exit code.
    verify_results(output)


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("command", choices=("configure", "bundle", "prepare", "run"))
    globals()[parser.parse_args().command]()
