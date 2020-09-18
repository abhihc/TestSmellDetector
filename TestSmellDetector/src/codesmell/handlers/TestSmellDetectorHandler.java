package codesmell.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.ui.IEditorPart;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.IDocument;

import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;

import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.texteditor.ITextEditor;

import java.util.Scanner;
import java.util.Stack;
import java.util.regex.Pattern;


public class TestSmellDetectorHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {

		IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindowChecked(event);

		// Initializing the display message as an empty string.
		String displayText = new String("");

		// Source code of the file currently opened in the editor.
		IEditorPart activeEditor = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
		ITextEditor textEditor = (ITextEditor)activeEditor;
		IDocument document = textEditor.getDocumentProvider().getDocument(textEditor.getEditorInput());
		String sourcecode = document.get(); 
		IFile file = ((FileEditorInput) activeEditor.getEditorInput()).getFile();

		// Remove the existing warnings respective to conditional code smell and execution delay. 
		try {
			for (IMarker marker : (file).findMarkers(IMarker.PROBLEM, true, 1)) {
				if ((((String) marker.getAttribute(IMarker.MESSAGE)).startsWith("Conditional")) || 
						(((String) marker.getAttribute(IMarker.MESSAGE)).startsWith("Test execution")) || 
						(((String) marker.getAttribute(IMarker.MESSAGE)).startsWith("External data"))) {
					marker.delete();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		// Scanning each line to find conditional statements and Thread.sleep() method.
		Scanner scanner = new Scanner(sourcecode);

		int lineNumber = 1;
		int testCount = 0;
		int conditionalSmellTestCount = 0;
		int executionDelaySmellTestCount = 0;
		int externalDataDepedendencySmellTestCount = 0;

		while (scanner.hasNextLine()) {

			boolean conditionalCodeSmellPresent = false;
			boolean executionDelayPresent = false;
			boolean externalDataDependencyPresent = false;

			String line = scanner.nextLine();

			Pattern testPattern = Pattern.compile("^([^//]*)(@Test)", Pattern.MULTILINE);
			Pattern testEndPattern = Pattern.compile("^([^//]*)[^\\w][\\}]$", Pattern.MULTILINE);

			// Regular expressions to check whether the code smell is present.
			Pattern conditionalStatementPattern = Pattern.compile("^([^//]*)(if|else|while|switch|for)[\\s]*[\\(]", Pattern.MULTILINE);
			Pattern executionDelayPattern = Pattern.compile("^([^//]*)(Thread\\.sleep)[\\s]*[\\(]", Pattern.MULTILINE);
			Pattern externalDataDependencyPattern = Pattern.compile("^([^//]*)(File|Connection)[\\s]*[a-zA-Z]+[\\s]*[\\=]", Pattern.MULTILINE);

			boolean ifTestPresent = testPattern.matcher(line).find();

			if(ifTestPresent) {
				testCount+=1;
				Stack<String> openBraces = new Stack<>();
				line = scanner.nextLine();
				lineNumber++;

				do {

					if (conditionalStatementPattern.matcher(line).find()) {
						conditionalCodeSmellPresent = true;
						openBraces.push("{");
						// Add marker to the lines where a conditional statement is present.
						try {
							IMarker marker = (file).createMarker(IMarker.PROBLEM);
							marker.setAttribute(IMarker.LINE_NUMBER, lineNumber);
							marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_WARNING);
							marker.setAttribute(IMarker.MESSAGE,
									"Conditional Test Logic smell is present in the test suite");
							marker.setAttribute(IMarker.PRIORITY, IMarker.PRIORITY_HIGH);
						} catch (Exception e) {
							e.printStackTrace();
						}

					}else if (externalDataDependencyPattern.matcher(line).find()) {
						externalDataDependencyPresent = true;

						// Add marker to the lines where external data dependency is present.
						try {
							IMarker marker = (file).createMarker(IMarker.PROBLEM);
							marker.setAttribute(IMarker.LINE_NUMBER, lineNumber);
							marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_WARNING);
							marker.setAttribute(IMarker.MESSAGE,
									"External Date Dependency smell is present in the test suite");
							marker.setAttribute(IMarker.PRIORITY, IMarker.PRIORITY_HIGH);
						} catch (Exception e) {
							e.printStackTrace();
						}
					}else if (executionDelayPattern.matcher(line).find()) {
						executionDelayPresent = true;

						// Add marker to the lines where Thread.sleep() method is present.
						try {
							IMarker marker = (file).createMarker(IMarker.PROBLEM);
							marker.setAttribute(IMarker.LINE_NUMBER, lineNumber);
							marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_WARNING);
							marker.setAttribute(IMarker.MESSAGE,
									"Test execution is halted by Thread.sleep() method");
							marker.setAttribute(IMarker.PRIORITY, IMarker.PRIORITY_HIGH);
						} catch (Exception e) {
							e.printStackTrace();
						}
					}

					line = scanner.nextLine();
					lineNumber++;

					while( (!(openBraces.empty())) && (testEndPattern.matcher(line).find()) ) {
						openBraces.pop();
						line = scanner.nextLine();
						lineNumber++;
					}


				}while( (!(testEndPattern.matcher(line).find())));

				if(conditionalCodeSmellPresent) {
					conditionalSmellTestCount+=1;
				}

				if(executionDelayPresent) {
					executionDelaySmellTestCount+=1;
				}

				if(externalDataDependencyPresent) {
					externalDataDepedendencySmellTestCount+=1;
				}
			}

			lineNumber++;
		}
		scanner.close();

		if(testCount!=0) {

			float conditionalSmellRatio = (float) (((testCount-conditionalSmellTestCount)*100)/testCount);
			float executionDelaySmellRatio = (float) (((testCount-executionDelaySmellTestCount)*100)/testCount);
			float externalDataDependencySmellRatio = (float) (((testCount-externalDataDepedendencySmellTestCount)*100)/testCount);

			displayText = "Ratio of tests without Conditional Test Logic smell = " + conditionalSmellRatio + "%" + "\n" + 
					"Ratio of tests without Test Execution Delay smell = " + executionDelaySmellRatio + "%" + "\n" + 
					"Ratio of tests without External Data Dependency smell = " + externalDataDependencySmellRatio + "%"; 

		}else {
			displayText = "There are no tests in the test suite";
		}





		MessageDialog.openInformation(window.getShell(), "Conditional Smell Detector Outcome", displayText);

		return null;
	}
}
