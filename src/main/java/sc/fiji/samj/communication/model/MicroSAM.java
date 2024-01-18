package sam.model;

public class MicroSAM implements SAMModel {

	@Override
	public String getName() {
		return "Micro SAM";
	}

	@Override
	public String getDescription() {
		return "Bla bla Micro SAM";
	}

	@Override
	public boolean isInstalled() {
		return false;
	}

}
