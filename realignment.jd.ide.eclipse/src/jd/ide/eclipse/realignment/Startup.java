package jd.ide.eclipse.realignment;

import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IFileEditorMapping;
import org.eclipse.ui.IStartup;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.internal.registry.EditorDescriptor;
import org.eclipse.ui.internal.registry.EditorRegistry;
import org.eclipse.ui.internal.registry.FileEditorMapping;

@SuppressWarnings("restriction")
public class Startup implements IStartup
{
  // The plug-in IDs
  public static final String EDITOR_ID = "realignment.editor.jd.ide.eclipse";
  public static final String JD_EDITOR_ID = "jd.ide.eclipse.editors.JDClassFileEditor";

  // External plug-in IDs
  public static final String JDT_EDITOR_ID = "org.eclipse.jdt.ui.ClassFileEditor";

  public void earlyStartup()
  {
    // Setup ".class" file association
    Display.getDefault().syncExec(new SetupClassFileAssociationRunnable());
  }

  private static class SetupClassFileAssociationRunnable implements Runnable
  {
    public void run()
    {
      EditorRegistry registry = (EditorRegistry) PlatformUI.getWorkbench()
          .getEditorRegistry();

      // Will not work because this will not persist across sessions
      // registry.setDefaultEditor("*.class", id);

      IFileEditorMapping[] mappings = registry.getFileEditorMappings();

      // Search Class file editor mappings
      IFileEditorMapping classNoSource = null;
      IFileEditorMapping classPlain = null;
      for (IFileEditorMapping mapping : mappings)
      {
        if (mapping.getExtension().equals("class without source"))
        {
          classNoSource = mapping;
        }
        else if (mapping.getExtension().equals("class"))
        {
          classPlain = mapping;
        }
      }
      IEditorDescriptor jdtClassViewer = registry.findEditor(JDT_EDITOR_ID);

      // * If there is a "class without source" type - handle this and revert
      // "class" to the default handler.
      // * Else register as the default handler for "class"

      if (classNoSource != null)
      {
        // Got a "class without source" type - default to handle this and
        // un-default from the "class" type
        registry.setDefaultEditor("." + classNoSource.getExtension(),
            EDITOR_ID);

        if (classPlain != null)
        {
          if (jdtClassViewer != null)
          {
            // Restore the default class viewer as the default
            // "class with source" viewer
            registry.setDefaultEditor("." + classPlain.getExtension(),
                JDT_EDITOR_ID);
          }

          for (IEditorDescriptor editorDesc : classPlain.getEditors())
          {
            if (editorDesc.getId().startsWith(JD_EDITOR_ID))
            {
              // Unmap the default JD Eclipse editor
              ((FileEditorMapping) classPlain)
                  .removeEditor((EditorDescriptor) editorDesc);
            }
          }
        }
      }
      else if (classPlain != null)
      {
        // Only got a class file type - default to decompile this
        registry.setDefaultEditor("." + classPlain.getExtension(), EDITOR_ID);
      }

      // Save updates
      registry.setFileEditorMappings((FileEditorMapping[]) mappings);
      registry.saveAssociations();
    }
  }
}
