/*
 * Copyright (C) 2013 University of Washington
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.opendatakit.aggregate.client;

import org.opendatakit.aggregate.client.OdkTablesTabUI.TablesChangeNotification;
import org.opendatakit.aggregate.client.table.OdkTablesTableList;

/**
 * This is the subtab that will house the display of the current ODK Tables
 * tables in the datastore. <br>
 * Based on OdkTablesAdminSubTab.
 *
 * @author sudar.sam@gmail.com
 *
 */
public class OdkTablesCurrentTablesSubTab extends AggregateSubTabBase implements TablesChangeNotification {

  private OdkTablesTabUI parent;
  
  private OdkTablesTableList tableList;

  public OdkTablesCurrentTablesSubTab(OdkTablesTabUI parent) {
    this.parent = parent;
    // vertical

    tableList = new OdkTablesTableList();
    add(tableList);

  }

  @Override
  public boolean canLeave() {
    return true;
  }

  /**
   * Update the displayed table page to reflect the contents of the datastore.
   */
  @Override
  public void update() {
    parent.update(this);
  }
  

  @Override
  public void updateTableSet(boolean tableListChanged) {
      tableList.updateTableList(parent.getTables(), tableListChanged);
      tableList.setVisible(true);
  }

}