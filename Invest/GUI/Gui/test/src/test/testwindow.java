package test;

import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import javax.bluetooth.UUID;
import org.eclipse.swt.widgets.Spinner;

import java.io.BufferedWriter;
import java.io.File;  // Import the File class
import java.io.FileWriter;
import java.io.IOException;  // Import the IOException class to handle errors
import java.io.PrintWriter;
import java.util.Date;



public class testwindow {

	protected Shell shell;
	private Label titleLabel;
	private Spinner penceTensSpinner;
	private Spinner penceUnitsSpinner;

	/**
	 * Launch the application.
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			testwindow window = new testwindow();
			window.open();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Open the window.
	 */
	public void open() {
		Display display = Display.getDefault();
		createContents();
		shell.open();
		shell.layout();
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}
	}

	/**
	 * Create contents of the window.
	 */
	protected void createContents() {
		shell = new Shell();
		shell.setSize(450, 300);
		shell.setText("Bluetooth Test");
		shell.setLayout(null);
		
		titleLabel = new Label(shell, SWT.NONE);
		titleLabel.setBounds(5, 5, 122, 14);
		titleLabel.setText("Input your money here");
		
		Spinner poundSpinner = new Spinner(shell, SWT.BORDER);
		poundSpinner.setBounds(15, 25, 37, 22);
		poundSpinner.setMaximum(5);
		
		penceTensSpinner = new Spinner(shell, SWT.BORDER);
		penceTensSpinner.setBounds(70, 25, 37, 22);
		penceTensSpinner.setMaximum(9);
		
		penceUnitsSpinner = new Spinner(shell, SWT.BORDER);
		penceUnitsSpinner.setMaximum(9);
		penceUnitsSpinner.setBounds(110, 25, 37, 22);
		
		Label poundLabel = new Label(shell, SWT.NONE);
		poundLabel.setBounds(0, 30, 59, 14);
		poundLabel.setText("£");
		
		Label pointLabel = new Label(shell, SWT.NONE);
		pointLabel.setText(".");
		pointLabel.setBounds(55, 30, 59, 14);
		
		Button submitButton = new Button(shell, SWT.NONE);
		submitButton.setBounds(33, 60, 94, 27);
		submitButton.setText("Submit");
		
		submitButton.addSelectionListener(new SelectionAdapter() {
 
            @Override
            public void widgetSelected(SelectionEvent arg0) {
                String pounds = poundSpinner.getText();
                String penceTens = penceTensSpinner.getText();
                String penceUnits = penceUnitsSpinner.getText();
                String result = "£"+pounds+"."+penceTens+penceUnits;
                System.out.println(result);
                
                Date now = new Date();
                String nowStr = now.toString();
                try {
                	File file = new File("results.txt");
                	FileWriter fr = new FileWriter(file, true);
	                BufferedWriter br = new BufferedWriter(fr);
	                PrintWriter pr = new PrintWriter(br);
	                pr.println(nowStr + " " + result);
	                pr.close();
	                System.out.println("Written");
                } catch (IOException e) {
                    System.out.println("An error occurred.");
                    e.printStackTrace();
                }
                System.exit(0);
            }
 
        });

	}
}
