package ai.nets.samj.ij.ui.commands;

public class DeleteRoiCommand implements Command {
  private RoiList model;
  private Mask mask;
  public DeleteRoiCommand(RoiList m, Mask roi) { model = m; mask = roi; }
  public void execute()   { model.remove(mask); }
  public void undo()      { model.add(mask); }
}
