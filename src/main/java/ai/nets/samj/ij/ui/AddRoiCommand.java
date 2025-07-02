package ai.nets.samj.ij.ui;
public class AddRoiCommand implements Command {
  private RoiList model;
  private Mask mask;
  public AddRoiCommand(RoiList m, Mask roi) { model = m; mask = roi; }
  public void execute()   { model.add(mask); }
  public void undo()      { model.remove(mask); }
}

