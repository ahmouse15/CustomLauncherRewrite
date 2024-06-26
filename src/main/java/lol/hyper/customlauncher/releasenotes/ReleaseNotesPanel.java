/*
 * This file is part of CustomLauncherRewrite.
 *
 * CustomLauncherRewrite is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * CustomLauncherRewrite is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with CustomLauncherRewrite.  If not, see <https://www.gnu.org/licenses/>.
 */

package lol.hyper.customlauncher.releasenotes;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class ReleaseNotesPanel extends JPanel {

    /**
     * Creates the release notes panel for displaying all updates.
     *
     * @param releaseNotesTracker The tracker used to fetch the updates.
     */
    public ReleaseNotesPanel(ReleaseNotesTracker releaseNotesTracker) {
        // GUI elements
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        JTable gameUpdatesTable = new JTable();
        String[] columns = new String[]{"Version", "Date"};

        DefaultTableModel gameUpdatedModel = (DefaultTableModel) gameUpdatesTable.getModel();
        gameUpdatedModel.setColumnIdentifiers(columns);
        gameUpdatesTable.setDefaultEditor(Object.class, null);
        gameUpdatesTable.getTableHeader().setReorderingAllowed(false);
        gameUpdatesTable.setFocusable(false);
        JScrollPane scrollPane = new JScrollPane(gameUpdatesTable);
        add(scrollPane);

        gameUpdatedModel.setRowCount(0);
        for (GameUpdate gameUpdate : releaseNotesTracker.getAllGameUpdates()) {
            String[] data = new String[]{gameUpdate.version(), gameUpdate.date()};
            gameUpdatedModel.addRow(data);
        }
        gameUpdatesTable.setModel(gameUpdatedModel);
        gameUpdatesTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                if (event.getClickCount() == 2) {
                    int selectedRow = gameUpdatesTable.getSelectedRow();
                    int versionColumnIndex = 0;

                    // allow clicking in any column
                    if (selectedRow != -1) {
                        String selectedVersion = (String) gameUpdatesTable.getValueAt(selectedRow, versionColumnIndex);
                        gameUpdatesTable.getSelectionModel().clearSelection();
                        // open the release notes for said version clicked
                        releaseNotesTracker.getAllGameUpdates().stream().filter(update -> selectedVersion.equalsIgnoreCase(update.version())).findAny().ifPresent(gameUpdate -> SwingUtilities.invokeLater(() -> new ReleaseNotesWindow(gameUpdate)));
                    }
                }
            }
        });
    }
}
