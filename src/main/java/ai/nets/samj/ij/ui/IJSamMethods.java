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
package ai.nets.samj.ij.ui;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import ai.nets.samj.gui.components.ComboBoxItem;
import ai.nets.samj.ui.UtilityMethods;
import ij.WindowManager;

/**
 * Implementation of the {@link UtilityMethods} interface providing the methods needed by the SAMJ interface 
 * that are specific to ImageJ
 * 
 * @author Carlos Garcia
 */
public class IJSamMethods implements UtilityMethods {

	@Override
	/**
	 * {@inheritDoc}
	 * 
	 * GEt the list of open images in ImageJ
	 */
	public List<ComboBoxItem> getListOfOpenImages() {
		return Arrays.stream(WindowManager.getImageTitles())
				.map(title -> new IJComboBoxItem(WindowManager.getImage(title).getID(), (Object) WindowManager.getImage(title)))
				.collect(Collectors.toList());
	}

}
