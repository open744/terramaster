import java.awt.Color;
import java.awt.Desktop;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
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
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.JEditorPane;

public class AboutDialog extends JDialog {

	Logger log = Logger.getLogger(this.getClass().getName());

	private final class HyperLinkListener implements HyperlinkListener {
		public void hyperlinkUpdate(HyperlinkEvent hle) {
			if (HyperlinkEvent.EventType.ACTIVATED.equals(hle.getEventType())) {
				System.out.println(hle.getURL());
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

	private final JTextField txtTerramaster = new JTextField();
	private JTextField textVersion;

	/**
	 * Create the application.
	 */
	public AboutDialog() {
		setAlwaysOnTop(true);
		initialize();
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		getContentPane().setFont(new Font("Tahoma", Font.PLAIN, 16));
		setBounds(100, 100, 540, 265);
		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		GridBagLayout gridBagLayout = new GridBagLayout();
		gridBagLayout.columnWidths = new int[] { 20, 10 };
		gridBagLayout.rowHeights = new int[] { 0, 0, 134, 37, 0 };
		gridBagLayout.columnWeights = new double[] { 0.0, 1.0 };
		gridBagLayout.rowWeights = new double[] { 0.0, 0.0, 1.0, 0.0, Double.MIN_VALUE };
		getContentPane().setLayout(gridBagLayout);

		JPanel panel = new JPanel();
		panel.setBorder(null);

		GridBagConstraints gbc_panel = new GridBagConstraints();
		gbc_panel.anchor = GridBagConstraints.WEST;
		gbc_panel.gridheight = 4;
		gbc_panel.insets = new Insets(0, 0, 0, 5);
		gbc_panel.gridx = 0;
		gbc_panel.gridy = 0;
		getContentPane().add(panel, gbc_panel);

		JLabel lblNewLabel = new JLabel("");
		lblNewLabel.setIcon(new ImageIcon("C:\\workspaces\\hochtief\\terramaster2\\resources\\about_icon.png"));
		panel.add(lblNewLabel);
		GridBagConstraints gbc_txtTerramaster = new GridBagConstraints();
		gbc_txtTerramaster.insets = new Insets(0, 0, 5, 0);
		gbc_txtTerramaster.gridx = 1;
		gbc_txtTerramaster.gridy = 0;
		txtTerramaster.setBorder(null);
		txtTerramaster.setFont(new Font("Tahoma", Font.PLAIN, 20));
		txtTerramaster.setHorizontalAlignment(SwingConstants.CENTER);
		txtTerramaster.setText("Terramaster");
		txtTerramaster.setEditable(false);
		getContentPane().add(txtTerramaster, gbc_txtTerramaster);
		txtTerramaster.setColumns(10);

		textVersion = new JTextField();
		textVersion.setHorizontalAlignment(SwingConstants.CENTER);
		textVersion.setFont(new Font("Tahoma", Font.PLAIN, 16));
		textVersion.setText("1.8");
		textVersion.setEditable(false);
		textVersion.setBorder(null);
		GridBagConstraints gbc_textVersion = new GridBagConstraints();
		gbc_textVersion.insets = new Insets(0, 0, 5, 0);
		gbc_textVersion.gridx = 1;
		gbc_textVersion.gridy = 1;
		getContentPane().add(textVersion, gbc_textVersion);
		textVersion.setColumns(10);

		JButton btnNewButton = new JButton("Ok");
		btnNewButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				setVisible(false);
			}
		});

		JPanel panel_1 = new JPanel();
		GridBagConstraints gbc_panel_1 = new GridBagConstraints();
		gbc_panel_1.fill = GridBagConstraints.BOTH;
		gbc_panel_1.gridx = 1;
		gbc_panel_1.gridy = 2;
		getContentPane().add(panel_1, gbc_panel_1);
		GridBagLayout gbl_panel_1 = new GridBagLayout();
		gbl_panel_1.columnWidths = new int[] { 134, 187, 10 };
		gbl_panel_1.rowHeights = new int[] { 27, 27, 27, 20, 0 };
		gbl_panel_1.columnWeights = new double[] { 1.0, 0.0, Double.MIN_VALUE };
		gbl_panel_1.rowWeights = new double[] { 0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE };
		panel_1.setLayout(gbl_panel_1);

		JLabel lblDevelopedBy = new JLabel("Developed by :");
		GridBagConstraints gbc_lblDevelopedBy = new GridBagConstraints();
		gbc_lblDevelopedBy.anchor = GridBagConstraints.EAST;
		gbc_lblDevelopedBy.insets = new Insets(0, 0, 5, 5);
		gbc_lblDevelopedBy.gridx = 0;
		gbc_lblDevelopedBy.gridy = 0;
		panel_1.add(lblDevelopedBy, gbc_lblDevelopedBy);

		JEditorPane btnReed = new JEditorPane("text/html", "<a href='https://github.com/open744'>reed</a>");
		btnReed.setEditable(false);
		btnReed.setBackground(Color.LIGHT_GRAY);
		btnReed.setForeground(new Color(0, 0, 153));
		btnReed.setOpaque(false);
		btnReed.addHyperlinkListener(new HyperLinkListener());
		GridBagConstraints gbc_btnReed = new GridBagConstraints();
		gbc_btnReed.fill = GridBagConstraints.BOTH;
		gbc_btnReed.insets = new Insets(0, 0, 5, 0);
		gbc_btnReed.gridx = 1;
		gbc_btnReed.gridy = 0;
		panel_1.add(btnReed, gbc_btnReed);

		JEditorPane btnPortreekid = new JEditorPane("text/html",
				"<a href='https://github.com/Portree-Kid'>portree_kid</a>");
		btnPortreekid.setEditable(false);
		btnPortreekid.setBackground(Color.LIGHT_GRAY);
		btnPortreekid.setOpaque(false);
		btnPortreekid.addHyperlinkListener(new HyperLinkListener());
		GridBagConstraints gbc_btnPortreekid = new GridBagConstraints();
		gbc_btnPortreekid.insets = new Insets(0, 0, 5, 0);
		gbc_btnPortreekid.fill = GridBagConstraints.BOTH;
		gbc_btnPortreekid.gridx = 1;
		gbc_btnPortreekid.gridy = 1;
		panel_1.add(btnPortreekid, gbc_btnPortreekid);

		JLabel lblLicense = new JLabel("License : ");
		GridBagConstraints gbc_lblLicense = new GridBagConstraints();
		gbc_lblLicense.anchor = GridBagConstraints.EAST;
		gbc_lblLicense.insets = new Insets(0, 0, 5, 5);
		gbc_lblLicense.gridx = 0;
		gbc_lblLicense.gridy = 2;
		panel_1.add(lblLicense, gbc_lblLicense);

		JEditorPane btnGpl = new JEditorPane("text/html",
				"<a href='https://github.com/Portree-Kid/terramaster/blob/master/COPYING'>GPL 2.0</a>");
		btnGpl.setBackground(Color.LIGHT_GRAY);
		btnGpl.setEditable(false);
		btnGpl.setOpaque(false);
		btnGpl.addHyperlinkListener(new HyperLinkListener());
		GridBagConstraints gbc_btnGpl = new GridBagConstraints();
		gbc_btnGpl.fill = GridBagConstraints.BOTH;
		gbc_btnGpl.insets = new Insets(0, 0, 5, 0);
		gbc_btnGpl.gridx = 1;
		gbc_btnGpl.gridy = 2;
		panel_1.add(btnGpl, gbc_btnGpl);

		JEditorPane btnSource = new JEditorPane("text/html",
				"<a href='https://github.com/Portree-Kid/terramaster'>Source</a>");
		btnSource.setBackground(Color.LIGHT_GRAY);
		btnSource.setEditable(false);
		btnSource.setOpaque(false);
		btnSource.addHyperlinkListener(new HyperLinkListener());

		GridBagConstraints gbc_btnSource = new GridBagConstraints();
		gbc_btnSource.fill = GridBagConstraints.BOTH;
		gbc_btnSource.gridx = 1;
		gbc_btnSource.gridy = 3;
		panel_1.add(btnSource, gbc_btnSource);
		GridBagConstraints gbc_btnNewButton = new GridBagConstraints();
		gbc_btnNewButton.gridx = 1;
		gbc_btnNewButton.gridy = 3;
		getContentPane().add(btnNewButton, gbc_btnNewButton);
	}
}
