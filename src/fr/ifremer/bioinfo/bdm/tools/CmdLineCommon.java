/* Copyright (C) 2018 Patrick G. Durand
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  You may obtain a copy of the License at
 *
 *     https://www.gnu.org/licenses/agpl-3.0.txt
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Affero General Public License for more details.
 */
package fr.ifremer.bioinfo.bdm.tools;

import bzh.plealog.dbmirror.util.conf.DBMSAbstractConfig;
import bzh.plealog.dbmirror.util.log.LoggerCentral;
import fr.ifremer.bioinfo.resources.CmdMessages;

/**
 * Utility methods for command line tools
 * 
 * @author Patrick G. Durand
 */
public class CmdLineCommon {

  /**
   * Provide a dedicated message to user if tool fails. In case of error,
   * this method calls system.exit with code 1.
   * 
   * @param jobInError true if tool fails, false otherwise
   * */
  public static void informForErrorMsg(boolean jobInError) {
    if (jobInError || LoggerCentral.errorMsgEmitted()) {
      String msg = String.format(CmdMessages.getString("Tool.msg1"), 
          DBMSAbstractConfig.getLogAppPath()+DBMSAbstractConfig.getLogAppFileName());
      System.err.println(msg);
      
      if (jobInError) {
        // exit code=1 : do this to report error to calling app
        System.exit(1);
      }
    }
  }

}
