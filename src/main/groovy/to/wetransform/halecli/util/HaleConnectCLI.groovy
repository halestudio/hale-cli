
/*
 * Copyright (c) 2018 wetransform GmbH
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution. If not, see <http://www.gnu.org/licenses/>.
 */
package to.wetransform.halecli.util

import static eu.esdihumboldt.hale.app.transform.ExecUtil.fail

import eu.esdihumboldt.hale.common.core.HalePlatform
import eu.esdihumboldt.hale.common.core.service.ServiceProvider
import eu.esdihumboldt.hale.io.haleconnect.HaleConnectService
import groovy.cli.picocli.CliBuilder
import groovy.cli.picocli.OptionAccessor

/**
 * Common utility functions for configuring a hale connect connection.
 *
 * @author Simon Templer
 */
class HaleConnectCLI {

  static void loginOptions(CliBuilder cli) {
    cli._(longOpt: 'hc-url', args:1, argName:'URL', 'Base URL for hale connect in case another instance than haleconnect.com should be used')

    cli._(longOpt: 'hc-user', args:1, argName:'User name', 'User name or email for logging into hale connect')
    cli._(longOpt: 'hc-password', args:1, argName:'Password', 'Password for logging into hale connect')
  }

  static void login(OptionAccessor options, HaleConnectService hc) {
    def baseUrl = options.'hc-url'
    if (baseUrl) {
      hc.basePathManager.setBaseUrl(baseUrl)
    }

    def user = options.'hc-user'
    def password = options.'hc-password'
    if (user) {
      if (hc.login(user, password)) {
        println 'Successfully logged in to hale connect'
      }
      else {
        fail('Failed to log in to hale connect')
      }
    }
  }

  static void login(OptionAccessor options, ServiceProvider services) {
    HaleConnectService hc = services.getService(HaleConnectService)
    if (hc == null) {
      fail('Unable to access hale connect client')
    }
    login(options, hc)
  }

  static void login(OptionAccessor options) {
    login(options, HalePlatform.getServiceProvider())
  }
}
