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
import java.net.URI;
import java.net.URISyntaxException;
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
		setBounds(100, 100, 452, 358);
		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		GridBagLayout gridBagLayout = new GridBagLayout();
		gridBagLayout.columnWidths = new int[] { 20, 10 };
		gridBagLayout.rowHeights = new int[] { 0, 134, 37, 0 };
		gridBagLayout.columnWeights = new double[] { 1.0, 1.0 };
		gridBagLayout.rowWeights = new double[] { 0.0, 1.0, 0.0, Double.MIN_VALUE };
		getContentPane().setLayout(gridBagLayout);

		JButton btnNewButton = new JButton("Ok");
		btnNewButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				setVisible(false);
			}
		});

		JPanel panel_1 = new JPanel();
		GridBagConstraints gbc_panel_1 = new GridBagConstraints();
		gbc_panel_1.anchor = GridBagConstraints.WEST;
		gbc_panel_1.gridwidth = 2;
		gbc_panel_1.insets = new Insets(0, 0, 5, 0);
		gbc_panel_1.fill = GridBagConstraints.VERTICAL;
		gbc_panel_1.gridx = 0;
		gbc_panel_1.gridy = 1;
		getContentPane().add(panel_1, gbc_panel_1);
		GridBagLayout gbl_panel_1 = new GridBagLayout();
		gbl_panel_1.columnWidths = new int[] {155, 180, 225, 0, 12};
		gbl_panel_1.rowHeights = new int[] { 43, 47, 0, 0 };
		gbl_panel_1.columnWeights = new double[] { 1.0, 1.0, 1.0, 0.0, Double.MIN_VALUE };
		gbl_panel_1.rowWeights = new double[] { 0.0, 1.0, 1.0, Double.MIN_VALUE };
		panel_1.setLayout(gbl_panel_1);
		
				JLabel lblLicense = new JLabel("License : ");
				GridBagConstraints gbc_lblLicense = new GridBagConstraints();
				gbc_lblLicense.anchor = GridBagConstraints.WEST;
				gbc_lblLicense.insets = new Insets(0, 5, 5, 5);
				gbc_lblLicense.gridx = 0;
				gbc_lblLicense.gridy = 1;
				panel_1.add(lblLicense, gbc_lblLicense);
		
				JEditorPane btnGpl = new JEditorPane("text/html",
						"<a href='https://github.com/Portree-Kid/terramaster/blob/master/COPYING'>GPL 2.0</a>");
				btnGpl.setFont(new Font("Arial", Font.PLAIN, 13));
				btnGpl.setBackground(Color.LIGHT_GRAY);
				btnGpl.setEditable(false);
				btnGpl.setOpaque(false);
				btnGpl.addHyperlinkListener(new HyperLinkListener());
				GridBagConstraints gbc_btnGpl = new GridBagConstraints();
				gbc_btnGpl.fill = GridBagConstraints.HORIZONTAL;
				gbc_btnGpl.insets = new Insets(0, 0, 5, 5);
				gbc_btnGpl.gridx = 1;
				gbc_btnGpl.gridy = 1;
				panel_1.add(btnGpl, gbc_btnGpl);
		
				JEditorPane btnSource = new JEditorPane("text/html",
						"<a href='https://github.com/Portree-Kid/terramaster'>Source</a>");
				btnSource.setFont(new Font("Arial", Font.PLAIN, 13));
				btnSource.setBackground(Color.LIGHT_GRAY);
				btnSource.setEditable(false);
				btnSource.setOpaque(false);
				btnSource.addHyperlinkListener(new HyperLinkListener());
				
						GridBagConstraints gbc_btnSource = new GridBagConstraints();
						gbc_btnSource.insets = new Insets(0, 0, 5, 0);
						gbc_btnSource.fill = GridBagConstraints.HORIZONTAL;
						gbc_btnSource.gridx = 2;
						gbc_btnSource.gridy = 1;
						panel_1.add(btnSource, gbc_btnSource);
						
						JLabel label = new JLabel("Developed by :");
						GridBagConstraints gbc_label = new GridBagConstraints();
						gbc_label.anchor = GridBagConstraints.WEST;
						gbc_label.insets = new Insets(0, 5, 5, 5);
						gbc_label.gridx = 0;
						gbc_label.gridy = 0;
						panel_1.add(label, gbc_label);
						
						JEditorPane editorPane = new JEditorPane("text/html", "<a href='https://github.com/open744'>reed</a>");
						editorPane.setToolTipText("Code");
						editorPane.setOpaque(false);
						editorPane.setForeground(new Color(0, 0, 153));
						editorPane.setEditable(false);
						editorPane.setBackground(Color.LIGHT_GRAY);
						GridBagConstraints gbc_editorPane = new GridBagConstraints();
						gbc_editorPane.insets = new Insets(0, 0, 5, 5);
						gbc_editorPane.fill = GridBagConstraints.HORIZONTAL;
						gbc_editorPane.gridx = 1;
						gbc_editorPane.gridy = 0;
						panel_1.add(editorPane, gbc_editorPane);
						
						JEditorPane editorPane_1 = new JEditorPane("text/html", "<a href='https://github.com/Portree-Kid'>portree_kid</a>");
						editorPane_1.setToolTipText("Code");
						editorPane_1.setOpaque(false);
						editorPane_1.setEditable(false);
						editorPane_1.setBackground(Color.LIGHT_GRAY);
						GridBagConstraints gbc_editorPane_1 = new GridBagConstraints();
						gbc_editorPane_1.insets = new Insets(0, 0, 5, 0);
						gbc_editorPane_1.fill = GridBagConstraints.HORIZONTAL;
						gbc_editorPane_1.gridx = 2;
						gbc_editorPane_1.gridy = 0;
						panel_1.add(editorPane_1, gbc_editorPane_1);
						
						JEditorPane dtrpnclive = new JEditorPane("text/html", "<a href='https://forum.flightgear.org/memberlist.php?mode=viewprofile&u=19112'>Clive2670</a>");
						dtrpnclive.setToolTipText("Logo");
						dtrpnclive.setOpaque(false);
						dtrpnclive.setEditable(false);
						dtrpnclive.setBackground(Color.LIGHT_GRAY);
						GridBagConstraints gbc_dtrpnclive = new GridBagConstraints();
						gbc_dtrpnclive.insets = new Insets(0, 0, 0, 5);
						gbc_dtrpnclive.fill = GridBagConstraints.BOTH;
						gbc_dtrpnclive.gridx = 3;
						gbc_dtrpnclive.gridy = 0;
						panel_1.add(dtrpnclive, gbc_dtrpnclive);
		
				JLabel lblNewLabel = new JLabel("");
				GridBagConstraints gbc_lblNewLabel = new GridBagConstraints();
				gbc_lblNewLabel.gridwidth = 2;
				gbc_lblNewLabel.anchor = GridBagConstraints.NORTH;
				gbc_lblNewLabel.insets = new Insets(0, 0, 0, 5);
				gbc_lblNewLabel.gridx = 0;
				gbc_lblNewLabel.gridy = 0;
				getContentPane().add(lblNewLabel, gbc_lblNewLabel);
				lblNewLabel.setIcon(new ImageIcon("C:\\workspaces\\hochtief\\terramaster2\\resources\\TerraMaster logo 2.png"));
		GridBagConstraints gbc_btnNewButton = new GridBagConstraints();
		gbc_btnNewButton.gridwidth = 2;
		gbc_btnNewButton.gridx = 0;
		gbc_btnNewButton.gridy = 2;
		getContentPane().add(btnNewButton, gbc_btnNewButton);
	}
}
