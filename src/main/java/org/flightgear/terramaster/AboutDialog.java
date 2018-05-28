package org.flightgear.terramaster;
import java.awt.Color;
import java.awt.Desktop;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

public class AboutDialog extends JDialog {
	Logger log = Logger.getLogger(TerraMaster.LOGGER_CATEGORY);
	private JLabel lblV;

	private final class HyperLinkListener implements HyperlinkListener {
		public void hyperlinkUpdate(HyperlinkEvent hle) {
			if (HyperlinkEvent.EventType.ACTIVATED.equals(hle.getEventType())) {
				log.fine("Calling " + hle.getURL().toExternalForm());
				try {
					Desktop.getDesktop().browse(new URI(hle.getURL().toExternalForm()));
				} catch (IOException e) {
					log.log(Level.WARNING, e.getMessage(), e);
				} catch (URISyntaxException e) {
					log.log(Level.WARNING, e.getMessage(), e);
				}
			}
		}
	}

	/**
	 * Create the application.
	 */
	public AboutDialog() {
		setIconImage(Toolkit.getDefaultToolkit().getImage("C:\\workspaces\\hochtief\\terramaster2\\resources\\TerraMaster logo cropped.ico"));
		setAlwaysOnTop(true);
		initialize();
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		getContentPane().setFont(new Font("Tahoma", Font.PLAIN, 16));
		setBounds(100, 100, 483, 379);
		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		GridBagLayout gridBagLayout = new GridBagLayout();
		gridBagLayout.columnWidths = new int[] { 149, 120 };
		gridBagLayout.rowHeights = new int[] { 0, 0, 0, 0, 37, 0 };
		gridBagLayout.columnWeights = new double[] { 0.0, 0.0 };
		gridBagLayout.rowWeights = new double[] { 0.0, 0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE };
		getContentPane().setLayout(gridBagLayout);

		JButton btnNewButton = new JButton("Ok");
		btnNewButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				setVisible(false);
			}
		});
		
		JLabel label = new JLabel("Developed by :");
		GridBagConstraints gbc_label = new GridBagConstraints();
		gbc_label.insets = new Insets(0, 0, 5, 5);
		gbc_label.gridx = 0;
		gbc_label.gridy = 1;
		getContentPane().add(label, gbc_label);
		
		JEditorPane editorPane = new JEditorPane("text/html", "<a href='https://github.com/open744'>reed</a>");
		GridBagConstraints gbc_editorPane = new GridBagConstraints();
		gbc_editorPane.insets = new Insets(0, 0, 5, 5);
		gbc_editorPane.gridx = 1;
		gbc_editorPane.gridy = 1;
		getContentPane().add(editorPane, gbc_editorPane);
		editorPane.setToolTipText("Code");
		editorPane.setOpaque(false);
		editorPane.setForeground(new Color(0, 0, 153));
		editorPane.setEditable(false);
		editorPane.setBackground(Color.LIGHT_GRAY);
		
		JEditorPane editorPane_1 = new JEditorPane("text/html", "<a href='https://github.com/Portree-Kid'>portree_kid</a>");
		GridBagConstraints gbc_editorPane_1 = new GridBagConstraints();
		gbc_editorPane_1.insets = new Insets(0, 0, 5, 5);
		gbc_editorPane_1.gridx = 2;
		gbc_editorPane_1.gridy = 1;
		getContentPane().add(editorPane_1, gbc_editorPane_1);
		editorPane_1.setToolTipText("Code");
		editorPane_1.setOpaque(false);
		editorPane_1.setEditable(false);
		editorPane_1.setBackground(Color.LIGHT_GRAY);
		
		JEditorPane dtrpnclive = new JEditorPane("text/html", "<a href='https://forum.flightgear.org/memberlist.php?mode=viewprofile&u=19112'>Clive2670</a>");
		GridBagConstraints gbc_dtrpnclive = new GridBagConstraints();
		gbc_dtrpnclive.insets = new Insets(0, 0, 5, 0);
		gbc_dtrpnclive.gridx = 3;
		gbc_dtrpnclive.gridy = 1;
		getContentPane().add(dtrpnclive, gbc_dtrpnclive);
		dtrpnclive.setToolTipText("Logo");
		dtrpnclive.setOpaque(false);
		dtrpnclive.setEditable(false);
		dtrpnclive.setBackground(Color.LIGHT_GRAY);
		
				JLabel lblLicense = new JLabel("License : ");
				GridBagConstraints gbc_lblLicense = new GridBagConstraints();
				gbc_lblLicense.insets = new Insets(0, 0, 5, 5);
				gbc_lblLicense.gridx = 0;
				gbc_lblLicense.gridy = 2;
				getContentPane().add(lblLicense, gbc_lblLicense);
		
				JEditorPane btnGpl = new JEditorPane("text/html",
						"<a href='https://github.com/Portree-Kid/terramaster/blob/master/COPYING'>GPL 2.0</a>");
				GridBagConstraints gbc_btnGpl = new GridBagConstraints();
				gbc_btnGpl.insets = new Insets(0, 0, 5, 5);
				gbc_btnGpl.gridx = 1;
				gbc_btnGpl.gridy = 2;
				getContentPane().add(btnGpl, gbc_btnGpl);
				btnGpl.setFont(new Font("Arial", Font.PLAIN, 13));
				btnGpl.setBackground(Color.LIGHT_GRAY);
				btnGpl.setEditable(false);
				btnGpl.setOpaque(false);
				btnGpl.addHyperlinkListener(new HyperLinkListener());
		
				JEditorPane btnSource = new JEditorPane("text/html",
						"<a href='https://github.com/Portree-Kid/terramaster'>Source</a>");
				GridBagConstraints gbc_btnSource = new GridBagConstraints();
				gbc_btnSource.insets = new Insets(0, 0, 5, 5);
				gbc_btnSource.gridx = 2;
				gbc_btnSource.gridy = 2;
				getContentPane().add(btnSource, gbc_btnSource);
				btnSource.setFont(new Font("Arial", Font.PLAIN, 13));
				btnSource.setBackground(Color.LIGHT_GRAY);
				btnSource.setEditable(false);
				btnSource.setOpaque(false);
				btnSource.addHyperlinkListener(new HyperLinkListener());
		
				JLabel lblNewLabel = new JLabel("");
				GridBagConstraints gbc_lblNewLabel = new GridBagConstraints();
				gbc_lblNewLabel.insets = new Insets(0, 0, 5, 0);
				gbc_lblNewLabel.gridwidth = 4;
				gbc_lblNewLabel.anchor = GridBagConstraints.NORTH;
				gbc_lblNewLabel.gridx = 0;
				gbc_lblNewLabel.gridy = 0;
				getContentPane().add(lblNewLabel, gbc_lblNewLabel);
				lblNewLabel.setIcon(new ImageIcon("C:\\workspaces\\hochtief\\terramaster2\\resources\\TerraMaster logo 2.png"));
		
		JLabel lblVersion = new JLabel("Version : ");
		GridBagConstraints gbc_lblVersion = new GridBagConstraints();
		gbc_lblVersion.insets = new Insets(0, 0, 5, 5);
		gbc_lblVersion.gridx = 0;
		gbc_lblVersion.gridy = 3;
		getContentPane().add(lblVersion, gbc_lblVersion);
		
		lblV = new JLabel("V");
		GridBagConstraints gbc_lblV = new GridBagConstraints();
		gbc_lblV.insets = new Insets(0, 0, 5, 5);
		gbc_lblV.gridx = 1;
		gbc_lblV.gridy = 3;
		getContentPane().add(lblV, gbc_lblV);
		GridBagConstraints gbc_btnNewButton = new GridBagConstraints();
		gbc_btnNewButton.gridwidth = 4;
		gbc_btnNewButton.gridx = 0;
		gbc_btnNewButton.gridy = 4;
		getContentPane().add(btnNewButton, gbc_btnNewButton);
		loadVersion();
	}

	private void loadVersion() {
		try {
			InputStream is = getClass().getResourceAsStream("/build_info.properties");
			Properties props = new Properties();
			props.load(is);
//			build.minor.number=10
//		    build.major.number=1
			String v = props.getProperty("build.major.number") + "." + props.getProperty("build.minor.number") ;
			lblV.setText(v);
			lblV.setToolTipText(props.getProperty("build.time"));
		} catch (Exception e) {
			// TODO: handle exception
		}
	}
}
