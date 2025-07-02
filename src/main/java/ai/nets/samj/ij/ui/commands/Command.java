package ai.nets.samj.ij.ui.commands;
public interface Command {
  void execute();   // do the action
  void undo();      // reverse it
}
