package sc.fiji.samj.communication.model;

import java.util.ArrayList;

public class SAMModels extends ArrayList<SAMModel> {

	public  SAMModels() {
		super();
		add(new EfficientSAM());
		add(new MicroSAM());
		add(new SAMViTHuge());
		add(new SAMViTLarge());
	}
}
