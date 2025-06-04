/*-
 * #%L
 * Plugin to help image annotation with SAM-based Deep Learning models
 * %%
 * Copyright (C) 2024 SAMJ developers.
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package ai.nets.samj.ij.utils;

import java.io.File;

import io.bioimage.modelrunner.system.PlatformDetection;

public class Constants {

    /**
     * The folder of Fiji
     */
	public static final String FIJI_FOLDER = getFijiFolder();
	
	private static String getFijiFolder() {
		File jvmFolder = new File(System.getProperty("java.home"));
		String imageJExecutable;
		if (PlatformDetection.isWindows())
			imageJExecutable = "fiji-windows-x64.exe";
		else if (PlatformDetection.isLinux())
			imageJExecutable = "fiji-linux-x64";
		else if (PlatformDetection.isMacOS() && PlatformDetection.getArch().equals(PlatformDetection.ARCH_ARM64))
			imageJExecutable = "Fiji.App/Contents/MacOS/fiji-macos-arm64";
		else if (PlatformDetection.isMacOS())
			imageJExecutable = "Fiji.App/Contents/MacOS/fiji-macos-x64";
		else
			throw new IllegalArgumentException("Unsupported Operating System");
		while (true && jvmFolder != null) {
			jvmFolder = jvmFolder.getParentFile();
			if (new File(jvmFolder + File.separator + imageJExecutable).isFile())
				return jvmFolder.getAbsolutePath();
		}
		return getImageJFolder();
	}
    
	private static String getImageJFolder() {
		File jvmFolder = new File(System.getProperty("java.home"));
		String imageJExecutable;
		if (PlatformDetection.isWindows())
			imageJExecutable = "ImageJ-win64.exe";
		else if (PlatformDetection.isLinux())
			imageJExecutable = "ImageJ-linux64";
		else if (PlatformDetection.isMacOS())
			imageJExecutable = "Contents/MacOS/ImageJ-macosx";
		else
			throw new IllegalArgumentException("Unsupported Operating System");
		while (true && jvmFolder != null) {
			jvmFolder = jvmFolder.getParentFile();
			if (new File(jvmFolder + File.separator + imageJExecutable).isFile())
				return jvmFolder.getAbsolutePath();
		}
		return new File("").getAbsolutePath();
		// TODO remove throw new RuntimeException("Unable to find the path to the ImageJ/Fiji being used.");
	}
}
