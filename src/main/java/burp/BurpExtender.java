package burp;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.*;
import javax.swing.JCheckBox;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;

import org.apache.commons.lang3.ArrayUtils;
import org.fife.ui.rsyntaxtextarea.FileLocation;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rsyntaxtextarea.TextEditorPane;
import org.fife.ui.rtextarea.RTextScrollPane;

import net.razorvine.pyro.*;


public class BurpExtender implements IBurpExtender, ITab, ActionListener, IContextMenuFactory, MouseListener, IExtensionStateListener, IIntruderPayloadProcessor,IHttpListener {

	public static final int PLATFORM_ANDROID = 0;
	public static final int PLATFORM_IOS = 1;
	public static final int PLATFORM_GENERIC = 2;

	private IBurpExtenderCallbacks callbacks;
	private IExtensionHelpers helpers;

	private PrintWriter stdout;
	private PrintWriter stderr;

	private JPanel mainPanel;

	private PyroProxy pyroBurpyService;
	private Process pyroServerProcess;

	private JTextField pythonPath;
	private String pythonScript;
	private JTextField pyroHost;
	private JTextField pyroPort;
	private JTextPane serverStatus;

	private JTextField burpyPath;
	private JCheckBox chckbxNewCheckBox;

	private JCheckBox chckbxEnc;
	private JCheckBox chckbxDec;
	private JCheckBox chckbxSign;
	private JCheckBox chckbxPro;
	private JCheckBox chckbxAuto;

	public JMenuItem itemEnc;
	public JMenuItem itemDec;

	public Boolean isAutoMain = false;
	public Boolean should_cus = false;
	public Boolean should_enc = false;
	public Boolean should_dec = false;
	public Boolean should_sign = false;
	public Boolean should_pro = false;
	public Boolean should_auto = false;


	private Style redStyle;
	private Style greenStyle;
	DefaultStyledDocument documentServerStatus;
	DefaultStyledDocument documentApplicationStatus;

	DefaultStyledDocument documentServerStatusButtons;
	DefaultStyledDocument documentApplicationStatusButtons;
	private JTextPane serverStatusButtons;


	private boolean serverStarted;
	private boolean applicationSpawned;
	private IContextMenuInvocation currentInvocation;
	private ITextEditor currentTextEditor;

	private ITextEditor stubTextEditor;


	private JButton loadPyFileButton;
	private JButton savePyFileButton;

	private JButton clearConsoleButton;
	private JButton reloadScript;

	private JEditorPane pluginConsoleTextArea;

	private TextEditorPane pyEditorTextArea;

	private Thread stdoutThread;
	private Thread stderrThread;


	private JTree tree;


	private boolean lastPrintIsJS;

	public void registerExtenderCallbacks(IBurpExtenderCallbacks c) {

		// Keep a reference to our callbacks object
		this.callbacks = c;

		// Obtain an extension helpers object
		helpers = callbacks.getHelpers();

		// Set our extension name
		callbacks.setExtensionName("Burpy");

		// register ourselves as an Intruder payload processor
		callbacks.registerIntruderPayloadProcessor(this);


		//register to produce options for the context menu
		callbacks.registerContextMenuFactory(this);

		// register to execute actions on unload
		callbacks.registerExtensionStateListener(this);

		// Initialize stdout and stderr
		stdout = new PrintWriter(callbacks.getStdout(), true);
		stderr = new PrintWriter(callbacks.getStderr(), true);

		stdout.println("Github: https://github.com/mr-m0nst3r/Burpy");
		stdout.println("");

		serverStarted = false;

		lastPrintIsJS = false;

		try {
			InputStream inputStream = getClass().getClassLoader().getResourceAsStream("res/burpyServicePyro.py");
			printSuccessMessage("got inputStream");
			BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream ));
			File outputFile = new File(System.getProperty("java.io.tmpdir") + System.getProperty("file.separator") + "burpyServicePyro.py");

			FileWriter fr = new FileWriter(outputFile);
			BufferedWriter br  = new BufferedWriter(fr);

			String s;
			while ((s = reader.readLine())!=null) {

				br.write(s);
				br.newLine();

			}
			reader.close();
			br.close();

			pythonScript = outputFile.getAbsolutePath();

		} catch(Exception e) {

			printException(e,"Error copying Pyro Server file");

		}

		SwingUtilities.invokeLater(new Runnable()  {

			@Override
			public void run()  {

				mainPanel = new JPanel();
				mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));

				JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);

				// **** Left panel (tabbed plus console)
				JSplitPane consoleTabbedSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);

				// Tabbed Pabel
				final JTabbedPane tabbedPanel = new JTabbedPane();
				tabbedPanel.addChangeListener(new ChangeListener() {
					public void stateChanged(ChangeEvent e) {

						SwingUtilities.invokeLater(new Runnable() {

							@Override
							public void run() {

								showHideButtons(tabbedPanel.getSelectedIndex());

							}
						});

					}
				});

				// **** TABS

				// **** CONFIGURATION PANEL

				JPanel configurationConfPanel = new JPanel();
				configurationConfPanel.setLayout(new BoxLayout(configurationConfPanel, BoxLayout.Y_AXIS));

				// RED STYLE
				StyleContext styleContext = new StyleContext();
				redStyle = styleContext.addStyle("red", null);
				StyleConstants.setForeground(redStyle, Color.RED);
				// GREEN STYLE
				greenStyle = styleContext.addStyle("green", null);
				StyleConstants.setForeground(greenStyle, Color.GREEN);

				JPanel serverStatusPanel = new JPanel();
				serverStatusPanel.setLayout(new BoxLayout(serverStatusPanel, BoxLayout.X_AXIS));
				serverStatusPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
				JLabel labelServerStatus = new JLabel("Server status: ");
				documentServerStatus = new DefaultStyledDocument();
				serverStatus = new JTextPane(documentServerStatus);
				try {
					documentServerStatus.insertString(0, "NOT running", redStyle);
				} catch (BadLocationException e) {
					printException(e,"Error setting labels");
				}

				serverStatus.setMaximumSize( serverStatus.getPreferredSize() );
				serverStatusPanel.add(labelServerStatus);
				serverStatusPanel.add(serverStatus);


				JPanel pythonPathPanel = new JPanel();
				pythonPathPanel.setLayout(new BoxLayout(pythonPathPanel, BoxLayout.X_AXIS));
				pythonPathPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
				JLabel labelPythonPath = new JLabel("Python binary path: ");
				pythonPath = new JTextField(200);
				if(callbacks.loadExtensionSetting("pythonPath") != null)
					pythonPath.setText(callbacks.loadExtensionSetting("pythonPath"));
				else {
					if(System.getProperty("os.name").startsWith("Windows")) {
						pythonPath.setText("C:\\python27\\python");
					} else {
						pythonPath.setText("/usr/bin/python");
					}
				}
				pythonPath.setMaximumSize( pythonPath.getPreferredSize() );
				JButton pythonPathButton = new JButton("Select file");
				pythonPathButton.setActionCommand("pythonPathSelectFile");
				pythonPathButton.addActionListener(BurpExtender.this);
				pythonPathPanel.add(labelPythonPath);
				pythonPathPanel.add(pythonPath);
				pythonPathPanel.add(pythonPathButton);

				JPanel pyroHostPanel = new JPanel();
				pyroHostPanel.setLayout(new BoxLayout(pyroHostPanel, BoxLayout.X_AXIS));
				pyroHostPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
				JLabel labelPyroHost = new JLabel("Pyro host: ");
				pyroHost = new JTextField(200);
				if(callbacks.loadExtensionSetting("pyroHost") != null)
					pyroHost.setText(callbacks.loadExtensionSetting("pyroHost"));
				else
					pyroHost.setText("localhost");
				pyroHost.setMaximumSize( pyroHost.getPreferredSize() );
				pyroHostPanel.add(labelPyroHost);
				pyroHostPanel.add(pyroHost);

				JPanel burpyPathPanel = new JPanel();
				burpyPathPanel.setLayout(new BoxLayout(burpyPathPanel, BoxLayout.X_AXIS));
				burpyPathPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
				JLabel labelBurpyPath = new JLabel("Burpy PY file path: ");
				burpyPath = new JTextField(200);
				if(callbacks.loadExtensionSetting("burpyPath") != null)
					burpyPath.setText(callbacks.loadExtensionSetting("burpyPath"));
				else {
					if(System.getProperty("os.name").startsWith("Windows")) {
						burpyPath.setText("C:\\burp\\script.py");
					} else {
						burpyPath.setText("/home/m0nst3r/work/scripts/jnBank.py");
					}
				}
				burpyPath.setMaximumSize( burpyPath.getPreferredSize() );
				JButton burpyPathButton = new JButton("Select file");
				burpyPathButton.setActionCommand("burpyPathSelectFile");
				burpyPathButton.addActionListener(BurpExtender.this);
//                JButton fridaDefaultPathButton = new JButton("Load default PY file");
//                fridaDefaultPathButton.setActionCommand("burpyPathSelectDefaultFile");
//                fridaDefaultPathButton.addActionListener(BurpExtender.this);
				burpyPathPanel.add(labelBurpyPath);
				burpyPathPanel.add(burpyPath);
				burpyPathPanel.add(burpyPathButton);
//                burpyPathPanel.add(fridaDefaultPathButton);

				JPanel pyroPortPanel = new JPanel();
				pyroPortPanel.setLayout(new BoxLayout(pyroPortPanel, BoxLayout.X_AXIS));
				pyroPortPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
				JLabel labelPyroPort = new JLabel("Pyro port: ");
				pyroPort = new JTextField(200);
				if(callbacks.loadExtensionSetting("pyroPort") != null)
					pyroPort.setText(callbacks.loadExtensionSetting("pyroPort"));
				else
					pyroPort.setText("19999");
				pyroPort.setMaximumSize( pyroPort.getPreferredSize() );
				pyroPortPanel.add(labelPyroPort);
				pyroPortPanel.add(pyroPort);

				// enc function
				chckbxEnc = new JCheckBox("Enable Encryption");
				chckbxEnc.setEnabled(true);
				chckbxEnc.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						if (chckbxEnc.isSelected()){
							should_enc = true;
						}else {
							should_enc = false;
						}
					}
				});

				// dec functioin
				chckbxDec = new JCheckBox("Enable Decryption");
				chckbxDec.setEnabled(true);
				chckbxDec.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						if (chckbxDec.isSelected()){
							should_dec = true;
						}else {
							should_dec = false;
						}
					}
				});

				// sign function
				chckbxSign = new JCheckBox("Enable Sign");
				chckbxSign.setEnabled(true);
				chckbxSign.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						if (chckbxSign.isSelected()){
							should_sign = true;
						}else {
							should_sign = false;
						}
					}
				});

				chckbxPro = new JCheckBox("Enable Processor");
				chckbxPro.setEnabled(true);
				chckbxPro.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						if (chckbxPro.isSelected()){
							should_pro = true;
						}else {
							should_pro = false;
						}
					}
				});

				chckbxAuto = new JCheckBox("Enable Auto Enc/Dec");
				chckbxAuto.setEnabled(true);
				chckbxAuto.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent actionEvent) {
						if (chckbxAuto.isSelected()){
							should_auto = true;
						} else {
							should_auto = false;
						}
					}
				});


				GridBagConstraints autoSignBox = new GridBagConstraints();
				autoSignBox.fill = GridBagConstraints.HORIZONTAL;
				autoSignBox.insets = new Insets(0, 0, 5, 0);
				autoSignBox.gridx = 1;
				autoSignBox.gridy = 3;

				GridBagConstraints shouldEncBox = new GridBagConstraints();
				shouldEncBox.fill = GridBagConstraints.HORIZONTAL;
				shouldEncBox.insets = new Insets(0, 0, 5, 0);
				shouldEncBox.gridx = 1;
				shouldEncBox.gridy = 3;

				GridBagConstraints shouldDecBox = new GridBagConstraints();
				shouldDecBox.fill = GridBagConstraints.HORIZONTAL;
				shouldDecBox.insets = new Insets(0, 0, 5, 0);
				shouldDecBox.gridx = 1;
				shouldDecBox.gridy = 3;

				GridBagConstraints shouldSignBox = new GridBagConstraints();
				shouldSignBox.fill = GridBagConstraints.HORIZONTAL;
				shouldSignBox.insets = new Insets(0, 0, 5, 0);
				shouldSignBox.gridx = 1;
				shouldSignBox.gridy = 3;

				GridBagConstraints shouldProBox = new GridBagConstraints();
				shouldProBox.fill = GridBagConstraints.HORIZONTAL;
				shouldProBox.insets = new Insets(0, 0, 5, 0);
				shouldProBox.gridx = 1;
				shouldProBox.gridy = 3;

				GridBagConstraints shouldAutoBox = new GridBagConstraints();
				shouldAutoBox.fill = GridBagConstraints.HORIZONTAL;
				shouldAutoBox.insets = new Insets(0, 0, 5, 0);
				shouldAutoBox.gridx = 1;
				shouldAutoBox.gridy = 3;


				configurationConfPanel.add(serverStatusPanel);
				configurationConfPanel.add(pythonPathPanel);
				configurationConfPanel.add(pyroHostPanel);
				configurationConfPanel.add(pyroPortPanel);
				configurationConfPanel.add(burpyPathPanel);
//				configurationConfPanel.add(chckbxNewCheckBox, autoSignBox);
				configurationConfPanel.add(chckbxEnc, shouldEncBox);
				configurationConfPanel.add(chckbxDec,shouldDecBox);
				configurationConfPanel.add(chckbxSign,shouldSignBox);
				configurationConfPanel.add(chckbxPro,shouldProBox);
				configurationConfPanel.add(chckbxAuto, shouldAutoBox);

				// **** END CONFIGURATION PANEL

				// **** PY EDITOR PANEL / CONSOLE
				pyEditorTextArea = new TextEditorPane();
				pyEditorTextArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_PYTHON);
				pyEditorTextArea.setCodeFoldingEnabled(false);
				RTextScrollPane sp = new RTextScrollPane(pyEditorTextArea);
				pyEditorTextArea.setFocusable(true);
				// **** END PY EDITOR PANEL / CONSOLE

				tabbedPanel.add("Configurations",configurationConfPanel);
				tabbedPanel.add("PY Editor",sp);

				// *** CONSOLE
				pluginConsoleTextArea = new JEditorPane("text/html", "<font color=\"green\"><b>*** Burpy Console ***</b></font><br/><br/>");
				JScrollPane scrollPluginConsoleTextArea = new JScrollPane(pluginConsoleTextArea);
				scrollPluginConsoleTextArea.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
				pluginConsoleTextArea.setEditable(false);

				consoleTabbedSplitPane.setTopComponent(tabbedPanel);
				consoleTabbedSplitPane.setBottomComponent(scrollPluginConsoleTextArea);
				consoleTabbedSplitPane.setResizeWeight(.7d);

				// *** RIGHT - BUTTONS

				// RIGHT
				JPanel rightSplitPane = new JPanel();
				rightSplitPane.setLayout(new GridBagLayout());
				GridBagConstraints gbc = new GridBagConstraints();
				gbc.gridwidth = GridBagConstraints.REMAINDER;
				gbc.fill = GridBagConstraints.HORIZONTAL;

				documentServerStatusButtons = new DefaultStyledDocument();
				serverStatusButtons = new JTextPane(documentServerStatusButtons);
				try {
					documentServerStatusButtons.insertString(0, "Server stopped", redStyle);
				} catch (BadLocationException e) {
					printException(e,"Error setting labels");
				}
				serverStatusButtons.setMaximumSize( serverStatusButtons.getPreferredSize() );

				documentApplicationStatusButtons = new DefaultStyledDocument();

				JButton startServer = new JButton("Start server");
				startServer.setActionCommand("startServer");
				startServer.addActionListener(BurpExtender.this);

				JButton killServer = new JButton("Kill server");
				killServer.setActionCommand("killServer");
				killServer.addActionListener(BurpExtender.this);

				JButton spawnApplication = new JButton("Spawn");
				spawnApplication.setActionCommand("spawnApplication");
				spawnApplication.addActionListener(BurpExtender.this);

				clearConsoleButton = new JButton("Clear console");
				clearConsoleButton.setActionCommand("clearConsole");
				clearConsoleButton.addActionListener(BurpExtender.this);

				reloadScript = new JButton("Reload Script");
				reloadScript.setActionCommand("reloadScript");
				reloadScript.addActionListener(BurpExtender.this);

				loadPyFileButton = new JButton("Load PY file");
				loadPyFileButton.setActionCommand("loadPyFile");
				loadPyFileButton.addActionListener(BurpExtender.this);

				savePyFileButton = new JButton("Save PY file");
				savePyFileButton.setActionCommand("savePyFile");
				savePyFileButton.addActionListener(BurpExtender.this);


				JSeparator separator = new JSeparator(SwingConstants.HORIZONTAL);
				separator.setBorder(BorderFactory.createMatteBorder(3, 0, 3, 0, Color.ORANGE));

				rightSplitPane.add(serverStatusButtons,gbc);
				rightSplitPane.add(startServer,gbc);
				rightSplitPane.add(killServer,gbc);
				// TODO
				rightSplitPane.add(spawnApplication,gbc);

				rightSplitPane.add(clearConsoleButton,gbc);

				rightSplitPane.add(reloadScript,gbc);

				rightSplitPane.add(separator,gbc);


				// TAB PY EDITOR
				rightSplitPane.add(loadPyFileButton,gbc);
				rightSplitPane.add(savePyFileButton,gbc);


				splitPane.setLeftComponent(consoleTabbedSplitPane);
				splitPane.setRightComponent(rightSplitPane);

				splitPane.setResizeWeight(.9d);

				mainPanel.add(splitPane);

				callbacks.customizeUiComponent(mainPanel);

				callbacks.addSuiteTab(BurpExtender.this);

			}

		});
		callbacks.registerHttpListener(this);

	}



	private void showHideButtons(int indexTabbedPanel) {

		switch(indexTabbedPanel) {

			// CONFIGURATIONS
			case 0:

				SwingUtilities.invokeLater(new Runnable() {

					@Override
					public void run() {

						loadPyFileButton.setVisible(false);
						savePyFileButton.setVisible(false);


					}

				});

				break;

			// PY editor
			case 1:

				SwingUtilities.invokeLater(new Runnable() {

					@Override
					public void run() {

						loadPyFileButton.setVisible(true);
						savePyFileButton.setVisible(true);

					}

				});

				break;

			default:
				printException(null,"ShowHideButtons: index not found");
				break;

		}

	}

	private void launchPyroServer(String pythonPath, String pyroServicePath) {

		Runtime rt = Runtime.getRuntime();

		String[] startServerCommand = {pythonPath,"-i",pyroServicePath,pyroHost.getText().trim(),pyroPort.getText().trim(),burpyPath.getText().trim()};

		try {
			documentServerStatus.insertString(0, "starting up ... ", redStyle);
			pyroServerProcess = rt.exec(startServerCommand);

			final BufferedReader stdOutput = new BufferedReader(new InputStreamReader(pyroServerProcess.getInputStream()));
			final BufferedReader stdError = new BufferedReader(new InputStreamReader(pyroServerProcess.getErrorStream()));

			// Initialize thread that will read stdout
			stdoutThread = new Thread() {

				public void run() {

					while(true) {

						try {

							final String line = stdOutput.readLine();

							// Only used to handle Pyro first message (when server start)
							if(line.equals("Ready.")) {

								pyroBurpyService = new PyroProxy(new PyroURI("PYRO:BurpyServicePyro@" + pyroHost.getText().trim() + ":" + pyroPort.getText().trim()));
								serverStarted = true;

								SwingUtilities.invokeLater(new Runnable() {

									@Override
									public void run() {

										serverStatus.setText("");
										serverStatusButtons.setText("");
										try {
											documentServerStatus.insertString(0, "running", greenStyle);
											documentServerStatusButtons.insertString(0, "Server running", greenStyle);
										} catch (BadLocationException e) {

											printException(e,"Exception setting labels");

										}

									}
								});

								printSuccessMessage("Pyro server started correctly");
								printSuccessMessage("Better use \"Kill Server\" after finished!");

								// Standard line
							} else {

								printJSMessage(line);

							}


						} catch (IOException e) {
							printException(e,"Error reading Pyro stdout");
						}

					}
				}

			};
			stdoutThread.start();

			// Initialize thread that will read stderr
			stderrThread = new Thread() {

				public void run() {

					while(true) {

						try {

							final String line = stdError.readLine();
							printException(null,line);

						} catch (IOException e) {

							printException(e,"Error reading Pyro stderr");

						}

					}
				}

			};
			stderrThread.start();

		} catch (final Exception e1) {

			printException(e1,"Exception starting Pyro server");

		}


	}

	public String getTabCaption() {

		return "Burpy";
	}

	public Component getUiComponent() {
		return mainPanel;
	}

	private void savePersistentSettings() {

		callbacks.saveExtensionSetting("pythonPath",pythonPath.getText().trim());
		callbacks.saveExtensionSetting("pyroHost",pyroHost.getText().trim());
		callbacks.saveExtensionSetting("pyroPort",pyroPort.getText().trim());
		callbacks.saveExtensionSetting("burpyPath",burpyPath.getText().trim());

	}




	public void actionPerformed(ActionEvent event) {

		String command = event.getActionCommand();



		if(command.equals("spawnApplication") && serverStarted) {

			try {

				final String s = (String)(pyroBurpyService.call("hello_spawn"));
				applicationSpawned = true;

				SwingUtilities.invokeLater(new Runnable() {

					@Override
					public void run() {


						printJSMessage("*** Output " + ":");
						printJSMessage(s);

					}
				});


			} catch (final Exception e) {

				printException(e,"Exception with spawn application");

			}



		} else if(command.equals("killServer") && serverStarted) {

			stdoutThread.stop();
			stderrThread.stop();

			try {
				pyroBurpyService.call("shutdown");
				pyroServerProcess.destroy();
				pyroBurpyService.close();
				serverStarted = false;

				SwingUtilities.invokeLater(new Runnable() {

					@Override
					public void run() {

						serverStatus.setText("");
						serverStatusButtons.setText("");
						try {
							documentServerStatus.insertString(0, "NOT running", redStyle);
							documentServerStatusButtons.insertString(0, "Server stopped", redStyle);
						} catch (BadLocationException e) {
							printException(e,"Exception setting labels");
						}

					}
				});

				printSuccessMessage("Pyro server shutted down");

			} catch (final Exception e) {

				printException(e,"Exception shutting down Pyro server");

			}


		} else if(command.equals("startServer") && !serverStarted) {

			File burpyFile = new File(burpyPath.getText().trim());
			if (burpyFile.exists()) {

				savePersistentSettings();

				try {

					launchPyroServer(pythonPath.getText().trim(), pythonScript);

				} catch (final Exception e) {

					printException(null, "Exception starting Pyro server");

				}
			}else {
				printException(null,"Burpy File not found!");

			}



		} else if(command.equals("loadPyFile")) {

			File pyFile = new File(burpyPath.getText().trim());
			final FileLocation fl = FileLocation.create(pyFile);

			SwingUtilities.invokeLater(new Runnable() {

				@Override
				public void run() {

					try {
						pyEditorTextArea.load(fl, null);
					} catch (IOException e) {
						printException(e,"Exception loading PY file");
					}

				}
			});

		} else if(command.equals("savePyFile")) {

			try {
				pyEditorTextArea.save();
				// TODO: stop pyro and start pyro

			} catch (IOException e) {
				printException(e,"Error saving PY file");
			}

		} else if(command.equals("contextcustom1")) {

			IHttpRequestResponse[] selectedItems = currentInvocation.getSelectedMessages();
			int[] selectedBounds = currentInvocation.getSelectionBounds();
			byte selectedInvocationContext = currentInvocation.getInvocationContext();
			String s = null;
			byte[] newHttp = null;
			List<String> headers = null;
			byte[] body = null;

			try {

				byte[] selectedRequestOrResponse = null;
				if (selectedInvocationContext == IContextMenuInvocation.CONTEXT_MESSAGE_EDITOR_REQUEST) {
					selectedRequestOrResponse = selectedItems[0].getRequest();
					IRequestInfo requestInfo = helpers.analyzeRequest(selectedRequestOrResponse);
					headers = new ArrayList<String>(requestInfo.getHeaders());
					String requestStr = new String(selectedRequestOrResponse);
					printSuccessMessage(requestStr.substring(requestInfo.getBodyOffset()));
					body = requestStr.substring(requestInfo.getBodyOffset()).getBytes();

				} else {
					selectedRequestOrResponse = selectedItems[0].getResponse();
					IResponseInfo responseInfo = helpers.analyzeResponse(selectedRequestOrResponse);
					String responseStr = new String(selectedRequestOrResponse);
					body = responseStr.substring(responseInfo.getBodyOffset()).getBytes();
				}

				s = (String)pyroBurpyService.call("hello", headers, new String[]{byteArrayToHexString(body)});


				if (selectedInvocationContext == IContextMenuInvocation.CONTEXT_MESSAGE_EDITOR_REQUEST) {
					newHttp = ArrayUtils.addAll(hexStringToByteArray(strToHexStr(s)));
					selectedItems[0].setRequest(newHttp);
				} else {
					final String msg = s.substring(s.indexOf("\r\n\r\n")+4);
					SwingUtilities.invokeLater(new Runnable() {

						@Override
						public void run() {

							JTextArea ta = new JTextArea(10, 30);
							ta.setText(msg);
							ta.setWrapStyleWord(true);
							ta.setLineWrap(true);
							ta.setCaretPosition(0);
							ta.setEditable(false);

							JOptionPane.showMessageDialog(null, new JScrollPane(ta), "Custom invocation response", JOptionPane.INFORMATION_MESSAGE);

						}

					});
				}

			} catch (Exception e) {

				printException(e, "Exception with custom context application");

			}
		}else if (command.equals("encrypt")) {
			IHttpRequestResponse[] selectedItems = currentInvocation.getSelectedMessages();
			int[] selectedBounds = currentInvocation.getSelectionBounds();
			byte selectedInvocationContext = currentInvocation.getInvocationContext();
			String s = null;
			byte[] newHttp = null;
			List<String> headers = null;
			byte[] body = null;

			try {

				byte[] selectedRequestOrResponse = null;
				if (selectedInvocationContext == IContextMenuInvocation.CONTEXT_MESSAGE_EDITOR_REQUEST) {
					selectedRequestOrResponse = selectedItems[0].getRequest();
					IRequestInfo requestInfo = helpers.analyzeRequest(selectedRequestOrResponse);
					headers = new ArrayList<String>(requestInfo.getHeaders());
					String requestStr = new String(selectedRequestOrResponse);
					printSuccessMessage(requestStr.substring(requestInfo.getBodyOffset()));
					body = requestStr.substring(requestInfo.getBodyOffset()).getBytes();

				} else {
					selectedRequestOrResponse = selectedItems[0].getResponse();
					IResponseInfo responseInfo = helpers.analyzeResponse(selectedRequestOrResponse);
					String responseStr = new String(selectedRequestOrResponse);
					body = responseStr.substring(responseInfo.getBodyOffset()).getBytes();
				}

				s = (String)pyroBurpyService.call("encrypt", headers, new String[]{byteArrayToHexString(body)});


				if (selectedInvocationContext == IContextMenuInvocation.CONTEXT_MESSAGE_EDITOR_REQUEST) {
					newHttp = ArrayUtils.addAll(hexStringToByteArray(strToHexStr(s)));
					selectedItems[0].setRequest(newHttp);
				} else {
					final String msg = s.substring(s.indexOf("\r\n\r\n")+4);
					SwingUtilities.invokeLater(new Runnable() {

						@Override
						public void run() {

							JTextArea ta = new JTextArea(10, 30);
							ta.setText(msg);
							ta.setWrapStyleWord(true);
							ta.setLineWrap(true);
							ta.setCaretPosition(0);
							ta.setEditable(false);

							JOptionPane.showMessageDialog(null, new JScrollPane(ta), "Custom invocation response", JOptionPane.INFORMATION_MESSAGE);

						}

					});
				}

			} catch (Exception e) {

				printException(e, "Exception with custom context application");

			}
		}else if (command.equals("decrypt")) {

			IHttpRequestResponse[] selectedItems = currentInvocation.getSelectedMessages();
			int[] selectedBounds = currentInvocation.getSelectionBounds();
			byte selectedInvocationContext = currentInvocation.getInvocationContext();
			String s = null;
			byte[] newHttp = null;
			List<String> headers = null;
			byte[] body = null;

			try {

				byte[] selectedRequestOrResponse = null;
				if (selectedInvocationContext == IContextMenuInvocation.CONTEXT_MESSAGE_EDITOR_REQUEST ||
						selectedInvocationContext == IContextMenuInvocation.CONTEXT_MESSAGE_VIEWER_REQUEST) {
					selectedRequestOrResponse = selectedItems[0].getRequest();
					IRequestInfo requestInfo = helpers.analyzeRequest(selectedRequestOrResponse);
					headers = new ArrayList<String>(requestInfo.getHeaders());
					String requestStr = new String(selectedRequestOrResponse);
					body = requestStr.substring(requestInfo.getBodyOffset()).getBytes();

				} else {
					selectedRequestOrResponse = selectedItems[0].getResponse();
					IResponseInfo responseInfo = helpers.analyzeResponse(selectedRequestOrResponse);
					headers = responseInfo.getHeaders();
					String responseStr = new String(selectedRequestOrResponse);
//					headers = new ArrayList();
//					headers.add("RESPONSE");
					body = responseStr.substring(responseInfo.getBodyOffset()).getBytes();
				}

				s = (String)pyroBurpyService.call("decrypt", headers, new String[]{byteArrayToHexString(body)});


				if (selectedInvocationContext == IContextMenuInvocation.CONTEXT_MESSAGE_EDITOR_REQUEST) {
					newHttp = ArrayUtils.addAll(hexStringToByteArray(strToHexStr(s)));
					selectedItems[0].setRequest(newHttp);
				}else if (selectedInvocationContext == IContextMenuInvocation.CONTEXT_MESSAGE_VIEWER_REQUEST ||
					selectedInvocationContext == IContextMenuInvocation.CONTEXT_MESSAGE_VIEWER_RESPONSE ) {
					final String msg = s.substring(s.indexOf("\r\n\r\n")+4);
					SwingUtilities.invokeLater(new Runnable() {

						@Override
						public void run() {

							JTextArea ta = new JTextArea(10, 30);
							ta.setText(msg);
							ta.setWrapStyleWord(true);
							ta.setLineWrap(true);
							ta.setCaretPosition(0);
							ta.setEditable(false);

							JOptionPane.showMessageDialog(null, new JScrollPane(ta), "Custom invocation", JOptionPane.INFORMATION_MESSAGE);

						}

					});
				}
//				else {
//					final String msg = s.substring(("RESPONSE").length()+2);
//
//					SwingUtilities.invokeLater(new Runnable() {
//
//						@Override
//						public void run() {
//
//							JTextArea ta = new JTextArea(10, 30);
//							ta.setText(msg);
//							ta.setWrapStyleWord(true);
//							ta.setLineWrap(true);
//							ta.setCaretPosition(0);
//							ta.setEditable(false);
//
//							JOptionPane.showMessageDialog(null, new JScrollPane(ta), "Custom invocation response", JOptionPane.INFORMATION_MESSAGE);
//
//						}
//
//					});
//				}

			} catch (Exception e) {

				printException(e, "Exception with custom context application");

			}
		}else if (command.equals("sign")) {

			IHttpRequestResponse[] selectedItems = currentInvocation.getSelectedMessages();
			int[] selectedBounds = currentInvocation.getSelectionBounds();
			byte selectedInvocationContext = currentInvocation.getInvocationContext();
			String s = null;
			byte[] newHttp = null;
			List<String> headers = null;
			byte[] body = null;

			try {

				byte[] selectedRequestOrResponse = null;
				if (selectedInvocationContext == IContextMenuInvocation.CONTEXT_MESSAGE_EDITOR_REQUEST) {
					selectedRequestOrResponse = selectedItems[0].getRequest();
					IRequestInfo requestInfo = helpers.analyzeRequest(selectedRequestOrResponse);
					headers = new ArrayList<String>(requestInfo.getHeaders());
					String requestStr = new String(selectedRequestOrResponse);
					body = requestStr.substring(requestInfo.getBodyOffset()).getBytes();

				} else {
					selectedRequestOrResponse = selectedItems[0].getResponse();
					IResponseInfo responseInfo = helpers.analyzeResponse(selectedRequestOrResponse);
					String responseStr = new String(selectedRequestOrResponse);
					body = responseStr.substring(responseInfo.getBodyOffset()).getBytes();
				}

				s = (String)pyroBurpyService.call("sign", headers, new String[]{byteArrayToHexString(body)});


				// Todo: set request/response accordingly and other commands
				if (selectedInvocationContext == IContextMenuInvocation.CONTEXT_MESSAGE_EDITOR_REQUEST) {
					newHttp = ArrayUtils.addAll(hexStringToByteArray(strToHexStr(s)));
					selectedItems[0].setRequest(newHttp);
				} else {
					final String msg = s;//.substring(("RESPONSE").length()+2);
					SwingUtilities.invokeLater(new Runnable() {

						@Override
						public void run() {

							JTextArea ta = new JTextArea(10, 30);
							ta.setText(msg);
							ta.setWrapStyleWord(true);
							ta.setLineWrap(true);
							ta.setCaretPosition(0);
							ta.setEditable(false);

							JOptionPane.showMessageDialog(null, new JScrollPane(ta), "Custom invocation response", JOptionPane.INFORMATION_MESSAGE);

						}

					});
				}

			} catch (Exception e) {

				printException(e, "Exception with custom context application");

			}

		} else if(command.equals("pythonPathSelectFile")) {

			JFrame parentFrame = new JFrame();
			JFileChooser fileChooser = new JFileChooser();
			fileChooser.setDialogTitle("Python Path");

			int userSelection = fileChooser.showOpenDialog(parentFrame);

			if(userSelection == JFileChooser.APPROVE_OPTION) {

				final File pythonPathFile = fileChooser.getSelectedFile();

				SwingUtilities.invokeLater(new Runnable() {

					@Override
					public void run() {
						pythonPath.setText(pythonPathFile.getAbsolutePath());
					}

				});

			}

		} else if(command.equals("burpyPathSelectFile")) {

			JFrame parentFrame = new JFrame();
			JFileChooser fileChooser = new JFileChooser();
			fileChooser.setDialogTitle("Burpy PY Path");

			int userSelection = fileChooser.showOpenDialog(parentFrame);

			if(userSelection == JFileChooser.APPROVE_OPTION) {

				final File burpyPathFile = fileChooser.getSelectedFile();

				SwingUtilities.invokeLater(new Runnable() {

					@Override
					public void run() {
						burpyPath.setText(burpyPathFile.getAbsolutePath());
					}

				});

			}

		} else if(command.startsWith("clearConsole")) {

			SwingUtilities.invokeLater(new Runnable() {

				@Override
				public void run() {
					String newConsoleText = "<font color=\"green\">";
					newConsoleText = newConsoleText + "<b>**** Console cleared successfully ****</b><br/>";
					newConsoleText = newConsoleText + "</font><br/>";

					pluginConsoleTextArea.setText(newConsoleText);

				}

			});

		} else if(command.startsWith("reloadScript")){
			if(serverStarted) {
				stdoutThread.stop();
				stderrThread.stop();

				try {
					pyroBurpyService.call("shutdown");
					pyroServerProcess.destroy();
					pyroBurpyService.close();
					serverStarted = false;

					SwingUtilities.invokeLater(new Runnable() {

						@Override
						public void run() {

							serverStatus.setText("");
							serverStatusButtons.setText("");
							try {
								documentServerStatus.insertString(0, "NOT running", redStyle);
								documentServerStatusButtons.insertString(0, "Server stopped", redStyle);
							} catch (BadLocationException e) {
								printException(e, "Exception setting labels");
							}

						}
					});

					printSuccessMessage("Pyro server shutted down");


				} catch (final Exception e) {

					printException(e, "Exception shutting down Pyro server");

				}
			}

			try {

				launchPyroServer(pythonPath.getText().trim(),pythonScript);

			} catch (final Exception e) {

				printException(null,"Exception starting Pyro server");

			}

		}

	}

	public List<JMenuItem> createMenuItems(IContextMenuInvocation invocation) {

		if(invocation.getInvocationContext() == IContextMenuInvocation.CONTEXT_MESSAGE_EDITOR_REQUEST ||
				invocation.getInvocationContext() == IContextMenuInvocation.CONTEXT_MESSAGE_EDITOR_RESPONSE) {

			currentInvocation = invocation;

			List<JMenuItem> menu = new ArrayList<JMenuItem>();

			JMenuItem itemCustom1 = new JMenuItem("Burpy Main");
			itemCustom1.setActionCommand("contextcustom1");
			itemCustom1.addActionListener(this);

			itemEnc = new JMenuItem("Burpy Enc");
			itemEnc.setActionCommand("encrypt");
			itemEnc.addActionListener(this);

			itemDec = new JMenuItem("Burpy Dec");
			itemDec.setActionCommand("decrypt");
			itemDec.addActionListener(this);

			JMenuItem itemSign = new JMenuItem("Burpy Sign");
			itemSign.setActionCommand("sign");
			itemSign.addActionListener(this);

			menu.add(itemCustom1);
			if (should_enc) {
				menu.add(itemEnc);
			}

			if (should_dec) {
				menu.add(itemDec);
			}

			if (should_sign) {
				menu.add(itemSign);
			}
			return menu;

		} else if(invocation.getInvocationContext() == IContextMenuInvocation.CONTEXT_MESSAGE_VIEWER_REQUEST ||
				invocation.getInvocationContext() == IContextMenuInvocation.CONTEXT_MESSAGE_VIEWER_RESPONSE) {

			currentInvocation = invocation;

			List<JMenuItem> menu = new ArrayList<JMenuItem>();

			JMenuItem itemCustom1 = new JMenuItem("Burpy Main");
			itemCustom1.setActionCommand("contextcustom1");
			itemCustom1.addActionListener(this);



			JMenuItem itemDec = new JMenuItem("Burpy Dec");
			itemDec.setActionCommand("decrypt");
			itemDec.addActionListener(this);



			menu.add(itemCustom1);

			if (should_dec) {
				menu.add(itemDec);
			}

			return menu;


		} else {

			return null;

		}

	}

	static String byteArrayToHexString(byte[] raw) {
		StringBuilder sb = new StringBuilder(2 + raw.length * 2);
		for (int i = 0; i < raw.length; i++) {
			sb.append(String.format("%02X", Integer.valueOf(raw[i] & 0xFF)));
		}
		return sb.toString();
	}

	private static byte[] hexStringToByteArray(String hex) {
		byte[] b = new byte[hex.length() / 2];
		for (int i = 0; i < b.length; i++){
			int index = i * 2;
			int v = Integer.parseInt(hex.substring(index, index + 2), 16);
			b[i] = (byte)v;
		}
		return b;
	}
	static String strToHexStr(String str) {
		char[] chars = "0123456789ABCDEF".toCharArray();
		StringBuilder sb = new StringBuilder("");
		byte[] bs = str.getBytes();
		int bit;
		for (int i=0; i<bs.length; i++) {
			bit = (bs[i] & 0x0f0) >>4;
			sb.append(chars[bit]);
			bit = bs[i] & 0x0f;
			sb.append(chars[bit]);
		}
		return sb.toString().trim();

	}


	@Override
	public void mouseClicked(MouseEvent e) {

	}

	@Override
	public void mousePressed(MouseEvent e) {

	}

	@Override
	public void mouseReleased(MouseEvent e) {

	}

	@Override
	public void mouseEntered(MouseEvent e) {
	}

	@Override
	public void mouseExited(MouseEvent e) {
	}

	public void printSuccessMessage(final String message) {

		SwingUtilities.invokeLater(new Runnable() {

			@Override
			public void run() {

				String oldConsoleText = pluginConsoleTextArea.getText();

				Pattern p = Pattern.compile("^.*<body>(.*)</body>.*$", Pattern.DOTALL);
				Matcher m = p.matcher(oldConsoleText);

				String newConsoleText = "";
				if(m.find()) {
					newConsoleText = m.group(1);
				}

				if(lastPrintIsJS) {
					newConsoleText = newConsoleText + "<br/>";
				}

				newConsoleText = newConsoleText + "<font color=\"green\">";
				newConsoleText = newConsoleText + "<b>" + message + "</b><br/>";
				newConsoleText = newConsoleText + "</font><br/>";

				pluginConsoleTextArea.setText(newConsoleText);

				lastPrintIsJS = false;

			}

		});

	}


	public void printJSMessage(final String message) {

		SwingUtilities.invokeLater(new Runnable() {

			@Override
			public void run() {

				String oldConsoleText = pluginConsoleTextArea.getText();
				Pattern p = Pattern.compile("^.*<body>(.*)</body>.*$", Pattern.DOTALL);
				Matcher m = p.matcher(oldConsoleText);

				String newConsoleText = "";
				if(m.find()) {
					newConsoleText = m.group(1);
				}

				newConsoleText = newConsoleText + "<font color=\"black\"><pre>";
				//newConsoleText = newConsoleText + message + "<br/>";
				newConsoleText = newConsoleText + message;
				newConsoleText = newConsoleText + "</pre></font>";

				pluginConsoleTextArea.setText(newConsoleText);

				lastPrintIsJS = true;

			}

		});

	}


	public void printException(final Exception e, final String message) {

		SwingUtilities.invokeLater(new Runnable() {

			@Override
			public void run() {


				String oldConsoleText = pluginConsoleTextArea.getText();
				Pattern p = Pattern.compile("^.*<body>(.*)</body>.*$", Pattern.DOTALL);
				Matcher m = p.matcher(oldConsoleText);

				String newConsoleText = "";
				if(m.find()) {
					newConsoleText = m.group(1);
				}

				if(lastPrintIsJS) {
					newConsoleText = newConsoleText + "<br/>";
				}

				newConsoleText = newConsoleText + "<font color=\"red\">";
				newConsoleText = newConsoleText + "<b>" + message + "</b><br/>";

				if(e != null) {
					newConsoleText = newConsoleText + e.toString() + "<br/>";
					//consoleText = consoleText + e.getMessage() + "<br/>";
					StackTraceElement[] exceptionElements = e.getStackTrace();
					for(int i=0; i< exceptionElements.length; i++) {
						newConsoleText = newConsoleText + exceptionElements[i].toString() + "<br/>";
					}
				}

				newConsoleText = newConsoleText + "</font><br/>";

				pluginConsoleTextArea.setText(newConsoleText);

				lastPrintIsJS = false;

			}

		});

	}


	@Override
	public void extensionUnloaded() {

		if(serverStarted) {

			stdoutThread.stop();
			stderrThread.stop();

			try {

				Runtime rt = Runtime.getRuntime();


				pyroBurpyService.call("shutdown");
				pyroServerProcess.destroy();
				pyroBurpyService.close();

				printSuccessMessage("Pyro server shutted down");

			} catch (final Exception e) {

				printException(e,"Exception shutting down Pyro server");

			}

		}

	}

	@Override
	public String getProcessorName()
	{
		return "Burpy processor";
	}

	@Override
	public byte[] processPayload(byte[] currentPayload, byte[] originalPayload, byte[] baseValue)
	{
		byte[] ret = currentPayload;
		if(should_pro){
			String pyroUrl = "PYRO:BridaServicePyro@" + pyroHost.getText() +":" + pyroPort.getText();
			
			try {
				PyroProxy pp = new PyroProxy(new PyroURI(pyroUrl));
				final String s = (String) (pyroBurpyService.call("processor", new String(currentPayload)));
				ret = s.getBytes();
				pp.close();
			} catch (Exception e) {
				stderr.println(e.toString());
				StackTraceElement[] exceptionElements = e.getStackTrace();
				for (int i = 0; i < exceptionElements.length; i++) {
					stderr.println(exceptionElements[i].toString());
				}
			}
		}

		return ret;
	}

	public void processHttpMessage(int toolFlag, boolean messageIsRequest, IHttpRequestResponse messageInfo) {
		List<String> headers = null;
		String pyroUrl = "PYRO:BridaServicePyro@" + pyroHost.getText() +":" + pyroPort.getText();
		if (should_auto) {
//			itemEnc.doClick();
			if (toolFlag == IBurpExtenderCallbacks.TOOL_SCANNER ||
					toolFlag == IBurpExtenderCallbacks.TOOL_REPEATER ||
					toolFlag == IBurpExtenderCallbacks.TOOL_INTRUDER) {
				if (messageIsRequest) {
//					itemEnc.doClick();
					// Get request bytes
					byte[] request = messageInfo.getRequest();
					// Get a IRequestInfo object, useful to work with the request
					IRequestInfo requestInfo = helpers.analyzeRequest(request);
					// Get the headers
					headers = requestInfo.getHeaders();
					// Get "test" parameter
					//IParameter contentParameter = helpers.getRequestParameter(request, "content");
					// Get body
					String requestStr = new String(request);
					int bodyOffset = requestInfo.getBodyOffset();
					byte[] body = requestStr.substring(bodyOffset).getBytes();
//					String ret = "";
					if(body != null) {
						//String urlDecodedContentParameterValue = helpers.urlDecode(contentParameter.getValue());
						String ret = "";
//						String pyroUrl = "PYRO:BridaServicePyro@localhost:19999";
						try {
							PyroProxy pp = new PyroProxy(new PyroURI(pyroUrl));

							ret = (String)pyroBurpyService.call("encrypt", headers, new String[]{byteArrayToHexString(body)});

							pp.close();
						} catch(Exception e) {
							stderr.println(e.toString());
							StackTraceElement[] exceptionElements = e.getStackTrace();
							for(int i=0; i< exceptionElements.length; i++) {
								stderr.println(exceptionElements[i].toString());
							}
						}
						// Create the new parameter
						//IParameter newTestParameter = helpers.buildParameter(contentParameter.getName(), helpers.urlEncode(ret), contentParameter.getType());
						// Create the new request with the updated parameter
						//byte[] newRequest = helpers.updateParameter(request, newTestParameter);
						IRequestInfo nreqInfo = helpers.analyzeRequest(ret.getBytes());
						headers = nreqInfo.getHeaders();
						int nbodyOff = nreqInfo.getBodyOffset();
						byte[] nbody = ret.substring(nbodyOff).getBytes();

						byte[] newRequest = helpers.buildHttpMessage(headers, nbody); //

						// Update the messageInfo object with the modified request (otherwise the request remains the old one)

						messageInfo.setRequest(newRequest);
					}
				}else {
//					itemDec.doClick();
					// Get request bytes
					byte[] request = messageInfo.getRequest();
					// Get a IRequestInfo object, useful to work with the request
					IRequestInfo requestInfo = helpers.analyzeRequest(request);
					// Get "test" parameter
					//IParameter contentParameter = helpers.getRequestParameter(request, "content");
					// Get body
					String requestStr = new String(request);

					byte[] reqbody = requestStr.substring(requestInfo.getBodyOffset()).getBytes();
					if(reqbody != null) {
						// Get response bytes
						byte[] response = messageInfo.getResponse();
						String responseStr = new String(response);
						// Get a IResponseInfo object, useful to work with the request
						IResponseInfo responseInfo = helpers.analyzeResponse(response);
						List<String> responseHeaders = responseInfo.getHeaders();
						// Get the offset of the body
						int bodyOffset = responseInfo.getBodyOffset();
						// Get the body (byte array and String)
//						byte[] body = Arrays.copyOfRange(response, bodyOffset, response.length);
						byte[] body = responseStr.substring(bodyOffset).getBytes();
//						String bodyString = helpers.bytesToString(body);
						String ret = "";
//						String pyroUrl = "PYRO:BridaServicePyro@localhost:19999";
						try {
							PyroProxy pp = new PyroProxy(new PyroURI(pyroUrl));
							ret = (String)pyroBurpyService.call("decrypt", responseHeaders, new String[]{byteArrayToHexString(body)});

							stderr.println(ret);
							pp.close();
						} catch(Exception e) {
							stderr.println(e.toString());
							StackTraceElement[] exceptionElements = e.getStackTrace();
							for(int i=0; i< exceptionElements.length; i++) {
								stderr.println(exceptionElements[i].toString());
							}
						}
						// Update the messageInfo object with the modified request (otherwise the request remains the old one)
//						byte[] newResponse = ArrayUtils.addAll(Arrays.copyOfRange(response, 0, bodyOffset),ret.getBytes());
						IResponseInfo nresInfo = helpers.analyzeResponse(ret.getBytes());
						int nbodyOff = nresInfo.getBodyOffset();
						byte[] nbody = ret.substring(nbodyOff).getBytes();
						byte[] newResponse = helpers.buildHttpMessage(responseHeaders, nbody);
						messageInfo.setResponse(newResponse);
					}
				}

			}

		}

	}

}

