package org.flightgear.terramaster;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.TreeSet;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

public class FlightPlan extends JDialog {

	private final JPanel contentPanel = new JPanel();
	private JComboBox<Airport> txtDeparture;
	private JComboBox<Airport> txtArrival;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		try {
			FlightPlan dialog = new FlightPlan();
			dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
			dialog.setVisible(true);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Create the dialog.
	 */
	public FlightPlan() {
		setModalityType(ModalityType.APPLICATION_MODAL);
		setTitle("Flightplan");
		setBounds(100, 100, 446, 162);
		getContentPane().setLayout(new BorderLayout());
		contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		getContentPane().add(contentPanel, BorderLayout.CENTER);
		GridBagLayout gbl_contentPanel = new GridBagLayout();
		gbl_contentPanel.columnWidths = new int[] { 0, 0, 0, 0 };
		gbl_contentPanel.rowHeights = new int[] { 0, 0, 0 };
		gbl_contentPanel.columnWeights = new double[] { 0.0, 1.0, 0.0, Double.MIN_VALUE };
		gbl_contentPanel.rowWeights = new double[] { 0.0, 0.0, Double.MIN_VALUE };
		contentPanel.setLayout(gbl_contentPanel);
		{
			JLabel lblDeparture = new JLabel("Departure : ");
			GridBagConstraints gbc_lblDeparture = new GridBagConstraints();
			gbc_lblDeparture.anchor = GridBagConstraints.EAST;
			gbc_lblDeparture.insets = new Insets(5, 5, 5, 5);
			gbc_lblDeparture.gridx = 0;
			gbc_lblDeparture.gridy = 0;
			contentPanel.add(lblDeparture, gbc_lblDeparture);
		}
		ActionListener getDepartureListener = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String searchString = txtDeparture.getSelectedItem().toString();
				txtDeparture.removeAllItems();
				txtDeparture.setEnabled(false);
				WebWorker w = new WebWorker(searchString, new AirportResult() {

					@Override
					public void addAirport(Airport result) {
						txtDeparture.addItem(result);
					}

					@Override
					public void done() {
						txtDeparture.setEnabled(true);
					}
				});
				w.execute();
			}
		};
		{
			txtDeparture = new JComboBox();
//			txtDeparture.addActionListener(getDepartureListener);
			txtDeparture.setEditable(true);
			GridBagConstraints gbc_txtDeparture = new GridBagConstraints();
			gbc_txtDeparture.insets = new Insets(5, 5, 5, 5);
			gbc_txtDeparture.fill = GridBagConstraints.HORIZONTAL;
			gbc_txtDeparture.gridx = 1;
			gbc_txtDeparture.gridy = 0;
			contentPanel.add(txtDeparture, gbc_txtDeparture);
		}
		{
			JButton button = new JButton(">");
			button.addActionListener(getDepartureListener);
			GridBagConstraints gbc_button = new GridBagConstraints();
			gbc_button.insets = new Insets(5, 5, 5, 5);
			gbc_button.gridx = 2;
			gbc_button.gridy = 0;
			contentPanel.add(button, gbc_button);
		}
		{
			JLabel lblArrival = new JLabel("Arrival : ");
			GridBagConstraints gbc_lblArrival = new GridBagConstraints();
			gbc_lblArrival.anchor = GridBagConstraints.EAST;
			gbc_lblArrival.insets = new Insets(5, 0, 0, 5);
			gbc_lblArrival.gridx = 0;
			gbc_lblArrival.gridy = 1;
			contentPanel.add(lblArrival, gbc_lblArrival);
		}
		ActionListener getArrivalListener = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String searchString = txtArrival.getSelectedItem().toString();
				txtArrival.removeAllItems();
				txtArrival.setEnabled(false);
				WebWorker w = new WebWorker(searchString, new AirportResult() {

					@Override
					public void addAirport(Airport result) {
						txtArrival.addItem(result);
					}

					@Override
					public void done() {
						txtArrival.setEnabled(true);
					}
				});
				w.execute();
			}
		};
		{
			txtArrival = new JComboBox();
			txtArrival.setEditable(true);
//			txtArrival.addActionListener(getArrivalListener);
			GridBagConstraints gbc_txtArrival = new GridBagConstraints();
			gbc_txtArrival.insets = new Insets(5, 5, 0, 5);
			gbc_txtArrival.fill = GridBagConstraints.HORIZONTAL;
			gbc_txtArrival.gridx = 1;
			gbc_txtArrival.gridy = 1;
			contentPanel.add(txtArrival, gbc_txtArrival);
		}
		{
			JButton button = new JButton(">");
			button.addActionListener(getArrivalListener);
			GridBagConstraints gbc_button = new GridBagConstraints();
			gbc_button.insets = new Insets(5, 5, 5, 5);
			gbc_button.gridx = 2;
			gbc_button.gridy = 1;
			contentPanel.add(button, gbc_button);
		}
		{
			JPanel buttonPane = new JPanel();
			buttonPane.setLayout(new FlowLayout(FlowLayout.RIGHT));
			getContentPane().add(buttonPane, BorderLayout.SOUTH);
			{
				JButton okButton = new JButton("OK");
				okButton.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						Airport selectedDeparture = (Airport) txtDeparture.getSelectedItem();
						Airport selectedArrival = (Airport) txtArrival.getSelectedItem();
						ArrayList<TileName> tiles = findAllTiles(selectedDeparture, selectedArrival);
						TerraMaster.frame.map.setSelection(tiles);
						TerraMaster.frame.map.repaint();
						setVisible(false);
					}

					private ArrayList<TileName> findAllTiles(Airport selectedDeparture, Airport selectedArrival) {
						double distance = CoordinateCalculation.greatCircleDistance(selectedDeparture.lat,
								selectedDeparture.lon, selectedArrival.lat, selectedArrival.lon);
						TreeSet<TileName> tiles = new TreeSet<>();
						double angle = Math.toRadians(CoordinateCalculation.greatCircleBearing(selectedDeparture.lat,
								selectedDeparture.lon, selectedArrival.lat, selectedArrival.lon));
						double newLat = selectedDeparture.lat;
						double newLon = selectedDeparture.lon;						
						for (double i = 0; i < distance; i += 0.1) {
							double dx = Math.cos(angle)/10;
							double dy = Math.sin(angle)/10;
							newLat += dx;
							newLon += dy;
							angle = Math.toRadians(CoordinateCalculation.greatCircleBearing(newLat, newLon, selectedArrival.lat, selectedArrival.lon));
							double newDistance = CoordinateCalculation.greatCircleDistance(newLat,
									newLon, selectedArrival.lat, selectedArrival.lon);
//							System.out.println(dx + "\t" + dy  + "\t" + Math.toDegrees(angle) + "\t" + newDistance);
							if( newDistance < 10)
								break;
							TileName tile = TileName.getTile(TileName.computeTileName(
									(int) newLat, (int) newLon));
							if (tile != null) {
								tiles.add(tile);
							}
						}
						ArrayList<TileName> ret = new ArrayList<>();
						ret.addAll(tiles);
						return ret;
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
	}

}
