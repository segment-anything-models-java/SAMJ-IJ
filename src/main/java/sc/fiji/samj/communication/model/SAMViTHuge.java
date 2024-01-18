package sam.model;

public class SAMViTHuge implements SAMModel {
	
	@Override
	public String getName() {
		return "ViT Huge";
	}

	@Override
	public String getDescription() {
		return "SAM Official ViT Huge";
	}

	@Override
	public boolean isInstalled() {
		return false;
	}
}
