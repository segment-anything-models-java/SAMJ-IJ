import os
from pathlib import Path
import stat
import sys
import tempfile
import unittest
from unittest.mock import patch
import zipfile

import fiji_ci


class FijiCITest(unittest.TestCase):
    def setUp(self):
        self.directory = tempfile.TemporaryDirectory()
        self.addCleanup(self.directory.cleanup)
        self.workspace = Path(self.directory.name)
        self.root = self.workspace / "Fiji"
        (self.root / "jars/linux64").mkdir(parents=True)
        (self.root / "plugins").mkdir()
        self.bundle = self.workspace / "ci-bundle"
        self.bundle.mkdir()

    def artifact(self, artifact_id, destination="jars"):
        name = artifact_id + "-2.0.jar"
        path = self.bundle / name
        path.write_bytes(b"source-built jar")
        return {"artifactId": artifact_id, "path": name, "sha256": fiji_ci.sha256(path),
                "destination": destination}

    def seed_sources(self):
        projects = {"samj": ("ai.nets", "samj", "0.0.5-SNAPSHOT"),
                    "jdll": ("io.bioimage", "dl-modelrunner", "0.6.4")}
        for name, (group, artifact, version) in projects.items():
            source = self.workspace / "ci-sources" / name
            (source / "target").mkdir(parents=True)
            (source / "pom.xml").write_text(
                '<project xmlns="%s"><groupId>%s</groupId><artifactId>%s</artifactId>'
                '<version>%s</version><properties><dl-modelrunner.version>0.6.3</dl-modelrunner.version>'
                '</properties></project>' % (fiji_ci.MAVEN_NS, group, artifact, version))
            jar_name = artifact + "-" + version + ".jar"
            built = source / "target" / jar_name
            built.write_bytes(b"built from source")
            staged = self.bundle / "jars" / group.replace(".", "/") / artifact / version
            staged.mkdir(parents=True)
            (staged / jar_name).write_bytes(built.read_bytes())
        (self.workspace / "pom.xml").write_text(
            '<project xmlns="%s"><groupId>ai.nets</groupId><artifactId>samj-IJ</artifactId>'
            '<version>0.0.4-SNAPSHOT</version></project>' % fiji_ci.MAVEN_NS)
        (self.bundle / "plugin").mkdir()
        (self.bundle / "plugin/samj-IJ-0.0.4-SNAPSHOT.jar").write_bytes(b"plugin")

    def test_configure_persists_jdll_override_and_snapshot_version(self):
        self.seed_sources()
        env = self.workspace / "github-env"
        with patch.dict(os.environ, GITHUB_ENV=str(env)):
            fiji_ci.configure(self.workspace)
        tree = fiji_ci.ET.parse(self.workspace / "ci-sources/samj/pom.xml")
        self.assertEqual(tree.findtext("m:properties/m:dl-modelrunner.version",
                                     namespaces={"m": fiji_ci.MAVEN_NS}), "0.6.4")
        self.assertIn("SAMJ_VERSION=0.0.5-SNAPSHOT", env.read_text())

    def test_bundle_records_exact_sources_and_maven_coordinates(self):
        self.seed_sources()
        with patch.object(fiji_ci.subprocess, "check_output", return_value="commit-sha\n"):
            fiji_ci.bundle(self.workspace)
        manifest = fiji_ci.json.loads((self.bundle / "manifest.json").read_text())
        samj, = [a for a in manifest["artifacts"] if a["artifactId"] == "samj"]
        self.assertEqual(samj["groupId"], "ai.nets")
        self.assertEqual(manifest["sources"]["jdll"]["commit"], "commit-sha")

    def test_bundle_rejects_published_jar_with_same_version(self):
        self.seed_sources()
        (self.bundle / "jars/ai/nets/samj/0.0.5-SNAPSHOT/samj-0.0.5-SNAPSHOT.jar").write_bytes(b"published jar")
        with patch.object(fiji_ci.subprocess, "check_output", return_value="commit-sha\n"):
            with self.assertRaisesRegex(RuntimeError, "differs from source build"):
                fiji_ci.bundle(self.workspace)

    def test_replaces_duplicate_jars_and_preserves_distinct_artifacts(self):
        old_names = ("jars/jna-1.0.jar", "jars/linux64/jna-1.0-linux.jar",
                     "plugins/SAMJ-IJ-1.0.jar", "jars/samj-1.0.jar")
        for name in old_names:
            (self.root / name).write_bytes(b"old")
        unrelated = self.root / "jars/jna-platform-1.0.jar"
        unrelated.write_bytes(b"keep")
        artifacts = [self.artifact("jna"), self.artifact("samj"),
                     self.artifact("samj-IJ", "plugins")]
        fiji_ci.install_jars(self.root, self.bundle, {"artifacts": artifacts})
        for name in old_names:
            self.assertFalse((self.root / name).exists())
        self.assertEqual(unrelated.read_bytes(), b"keep")
        for artifact in artifacts:
            installed = self.root / artifact["destination"] / artifact["path"]
            self.assertEqual(fiji_ci.sha256(installed), artifact["sha256"])

    def test_bad_bundle_does_not_remove_existing_jars(self):
        old = self.root / "jars/jna-1.0.jar"
        old.write_bytes(b"keep")
        artifact = self.artifact("jna")
        (self.bundle / artifact["path"]).write_bytes(b"corrupt")
        with self.assertRaisesRegex(RuntimeError, "checksum mismatch"):
            fiji_ci.install_jars(self.root, self.bundle, {"artifacts": [artifact]})
        self.assertEqual(old.read_bytes(), b"keep")

    def test_extraction_preserves_executable_and_mac_bundle_paths(self):
        archive = self.workspace / "fiji.zip"
        executable = zipfile.ZipInfo("Fiji/Fiji.app/Contents/MacOS/fiji-macos-arm64")
        executable.external_attr = (stat.S_IFREG | 0o755) << 16
        with zipfile.ZipFile(archive, "w") as zipped:
            zipped.writestr(executable, b"launcher")
        destination = self.workspace / "extracted"
        fiji_ci.extract_archive(archive, destination)
        launcher = destination / executable.filename
        self.assertEqual(launcher.read_bytes(), b"launcher")
        if os.name != "nt":
            self.assertTrue(launcher.stat().st_mode & stat.S_IXUSR)

    @unittest.skipIf(os.name == "nt", "Unix JDK symlinks")
    def test_extraction_preserves_jdk_symlinks(self):
        archive = self.workspace / "fiji.zip"
        link = zipfile.ZipInfo("Fiji/java/lib/alias")
        link.external_attr = (stat.S_IFLNK | 0o777) << 16
        with zipfile.ZipFile(archive, "w") as zipped:
            zipped.writestr("Fiji/java/lib/library", b"native library")
            zipped.writestr(link, "library")
        destination = self.workspace / "extracted"
        fiji_ci.extract_archive(archive, destination)
        self.assertTrue((destination / link.filename).is_symlink())
        self.assertEqual((destination / link.filename).read_bytes(), b"native library")

    def test_bad_fiji_checksum_fails_before_extraction(self):
        (self.workspace / "fiji.zip").write_bytes(b"bad download")
        (self.workspace / "fiji.zip.sha256").write_text("0" * 64)
        with self.assertRaisesRegex(RuntimeError, "SHA-256"):
            fiji_ci.prepare(self.workspace)
        self.assertFalse((self.workspace / "fiji").exists())

    def test_prepare_discovers_all_platform_launchers(self):
        for launcher in ("fiji-linux-x64", "fiji-windows-x64.exe", "fiji-macos-arm64", "fiji-macos-x64"):
            with self.subTest(launcher=launcher):
                workspace = self.workspace / launcher
                workspace.mkdir()
                (workspace / "ci-bundle").mkdir()
                fiji_ci.write_json(workspace / "ci-bundle/manifest.json", {"artifacts": []})
                relative = "Fiji/" + ("Fiji.app/Contents/MacOS/" if "macos" in launcher else "") + launcher
                archive = workspace / "fiji.zip"
                with zipfile.ZipFile(archive, "w") as zipped:
                    zipped.writestr(relative, b"launcher")
                    zipped.writestr("Fiji/jars/keep.jar", b"jar")
                    zipped.writestr("Fiji/plugins/keep.jar", b"plugin")
                (workspace / "fiji.zip.sha256").write_text(fiji_ci.sha256(archive))
                with patch.dict(os.environ, FIJI_LAUNCHER=launcher):
                    fiji_ci.prepare(workspace)
                config = fiji_ci.json.loads((workspace / "test-output/fiji.json").read_text())
                self.assertEqual(Path(config["root"]), workspace / "fiji/Fiji")
                self.assertEqual(Path(config["launcher"]), workspace / "fiji" / relative)

    def test_missing_markers_and_empty_mask_fail(self):
        output = self.workspace / "test-output"
        output.mkdir()
        with self.assertRaises(FileNotFoundError):
            fiji_ci.verify_results(output)
        fiji_ci.write_json(output / "installation.json", {"installed": True})
        fiji_ci.write_json(output / "inference.json", {"foreground_pixels": 0})
        (output / "mask.tif").write_bytes(b"TIFF")
        with self.assertRaisesRegex(RuntimeError, "inference failed"):
            fiji_ci.verify_results(output)

    def test_zero_exit_without_script_results_fails(self):
        output = self.workspace / "test-output"
        output.mkdir()
        fiji_ci.write_json(output / "fiji.json", {"root": str(self.root), "launcher": "fiji"})
        with patch.object(fiji_ci, "launch") as launch:
            with self.assertRaises(FileNotFoundError):
                fiji_ci.run(self.workspace)
        launch.assert_called_once()

    def test_subprocess_failure_and_timeout_are_reported(self):
        log = self.workspace / "fiji.log"
        with self.assertRaisesRegex(RuntimeError, "code 3"):
            fiji_ci.launch([sys.executable, "-c", "print('error'); raise SystemExit(3)"],
                           self.workspace, os.environ, log, 10)
        self.assertIn("error", log.read_text())
        with self.assertRaisesRegex(RuntimeError, "timed out"):
            fiji_ci.launch([sys.executable, "-c", "import time; time.sleep(10)"],
                           self.workspace, os.environ, log, 0.1)


if __name__ == "__main__":
    unittest.main()
