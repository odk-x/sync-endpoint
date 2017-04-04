/*
 * Copyright (C) 2011 University of Washington
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

import org.opendatakit.aggregate.client.permissions.AccessInfoSheet;
import org.opendatakit.aggregate.constants.common.UIConsts;

public class GroupsAndRolesSubTab extends AggregateSubTabBase {

  private AccessInfoSheet accessConfig;

  public GroupsAndRolesSubTab() {
    // vertical
    setStylePrimaryName(UIConsts.VERTICAL_FLOW_PANEL_STYLENAME);

  }

  @Override
  public boolean canLeave() {
	  return true;
  }

  @Override
  public void update() {
	if ( accessConfig == null ) {
		accessConfig = new AccessInfoSheet(this);
		add(accessConfig);
	}
	accessConfig.setVisible(true);
  }
}
