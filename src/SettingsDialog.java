import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

public class SettingsDialog extends JDialog {

	private final JPanel contentPanel = new JPanel();
	private JTextField txtScenerypath;
	private JComboBox cmbScenerySource;

	/**
	 * Create the dialog.
	 */
	public SettingsDialog() {
		setTitle("Settings");
		setModal(true);
		setBounds(100, 100, 640, 142);
		getContentPane().setLayout(new BorderLayout());
		contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		getContentPane().add(contentPanel, BorderLayout.CENTER);
		GridBagLayout gbl_contentPanel = new GridBagLayout();
		gbl_contentPanel.columnWidths = new int[] { 0, 0, 0, 0 };
		gbl_contentPanel.rowHeights = new int[] { 0, 22, 0 };
		gbl_contentPanel.columnWeights = new double[] { 0.0, 1.0, 0.0,
				Double.MIN_VALUE };
		gbl_contentPanel.rowWeights = new double[] { 0.0, 0.0, Double.MIN_VALUE };
		contentPanel.setLayout(gbl_contentPanel);
		{
			JLabel lblSceneryPath = new JLabel("Scenery Path :");
			GridBagConstraints gbc_lblSceneryPath = new GridBagConstraints();
			gbc_lblSceneryPath.insets = new Insets(0, 0, 5, 5);
			gbc_lblSceneryPath.anchor = GridBagConstraints.EAST;
			gbc_lblSceneryPath.gridx = 0;
			gbc_lblSceneryPath.gridy = 0;
			contentPanel.add(lblSceneryPath, gbc_lblSceneryPath);
		}
		{
			txtScenerypath = new JTextField();
			GridBagConstraints gbc_txtScenerypath = new GridBagConstraints();
			gbc_txtScenerypath.insets = new Insets(0, 0, 5, 5);
			gbc_txtScenerypath.fill = GridBagConstraints.HORIZONTAL;
			gbc_txtScenerypath.gridx = 1;
			gbc_txtScenerypath.gridy = 0;
			contentPanel.add(txtScenerypath, gbc_txtScenerypath);
			txtScenerypath.setColumns(10);
		}
		{
			final JButton selectDirectoryButton = new JButton("...");
			selectDirectoryButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					JFileChooser fc = new JFileChooser();
					fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
					fc.setCurrentDirectory(new File(txtScenerypath.getText()));

					if (fc.showOpenDialog(selectDirectoryButton) == JFileChooser.APPROVE_OPTION) {
						File f = fc.getSelectedFile();
						fc.setCurrentDirectory(f);
						txtScenerypath.setText(f.getAbsolutePath());
					}

				}
			});
			GridBagConstraints gbc_selectDirectoryButton = new GridBagConstraints();
			gbc_selectDirectoryButton.insets = new Insets(0, 0, 5, 0);
			gbc_selectDirectoryButton.gridx = 2;
			gbc_selectDirectoryButton.gridy = 0;
			contentPanel.add(selectDirectoryButton, gbc_selectDirectoryButton);
		}
		{
			JLabel lblScenerySource = new JLabel("Scenery Source :");
			GridBagConstraints gbc_lblScenerySource = new GridBagConstraints();
			gbc_lblScenerySource.anchor = GridBagConstraints.EAST;
			gbc_lblScenerySource.insets = new Insets(0, 0, 0, 5);
			gbc_lblScenerySource.gridx = 0;
			gbc_lblScenerySource.gridy = 1;
			contentPanel.add(lblScenerySource, gbc_lblScenerySource);
		}
		{
			cmbScenerySource = new JComboBox();
			cmbScenerySource.setModel(new DefaultComboBoxModel(new String[] {
					"Terrasync (SVN)", "HTTP" }));
			cmbScenerySource.setSelectedItem(TerraMaster.props.getProperty(TerraMasterProperties.SERVER_TYPE));
			GridBagConstraints gbc_comboBox = new GridBagConstraints();
			gbc_comboBox.insets = new Insets(0, 0, 0, 5);
			gbc_comboBox.fill = GridBagConstraints.HORIZONTAL;
			gbc_comboBox.gridx = 1;
			gbc_comboBox.gridy = 1;
			contentPanel.add(cmbScenerySource, gbc_comboBox);
		}
		{
			JPanel buttonPane = new JPanel();
			buttonPane.setLayout(new FlowLayout(FlowLayout.RIGHT));
			getContentPane().add(buttonPane, BorderLayout.SOUTH);
			{
				JButton okButton = new JButton("OK");
				okButton.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						setVisible(false);
						try {
							TerraMaster.mapScenery = TerraMaster.newScnMap(txtScenerypath.getText());
							TerraMaster.frame.map.repaint();
							TerraMaster.props.setProperty(TerraMasterProperties.SCENERY_PATH,
									txtScenerypath.getText());
							TerraMaster.props.setProperty(TerraMasterProperties.SERVER_TYPE, (String) cmbScenerySource.getSelectedItem());
							TerraMaster.setTileService();
							TerraMaster.svn.setScnPath(new File(txtScenerypath.getText()));
						} catch (Exception x) {
							x.printStackTrace();
						}
						
					}
				});
				okButton.setActionCommand("OK");
				buttonPane.add(okButton);
				getRootPane().setDefaultButton(okButton);
			}
			{
				JButton cancelButton = new JButton("Cancel");
				cancelButton.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						setVisible(false);
					}
				});
				cancelButton.setActionCommand("Cancel");
				buttonPane.add(cancelButton);
			}
		}
		txtScenerypath.setText((String) TerraMaster.props.get(TerraMasterProperties.SCENERY_PATH));
		{
			JButton addSettingsButton = new JButton("...");
			addSettingsButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					
				}
			});
			GridBagConstraints gbc_addSettingsButton = new GridBagConstraints();
			gbc_addSettingsButton.gridx = 2;
			gbc_addSettingsButton.gridy = 1;
			contentPanel.add(addSettingsButton, gbc_addSettingsButton);
		}
	}

}
