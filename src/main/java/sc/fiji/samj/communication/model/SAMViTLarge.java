package sam.model;

public class SAMViTLarge implements SAMModel {
	
	@Override
	public String getName() {
		return "ViT Large";
	}

	@Override
	public String getDescription() {
		return "Bla bla SAM Official ViT";
	}

	@Override
	public boolean isInstalled() {
		return false;
	}
}
