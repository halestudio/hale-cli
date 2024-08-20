
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
package to.wetransform.halecli.project.match.postnas;

import to.wetransform.halecli.project.match.MatchSchemasCommand;
import to.wetransform.halecli.project.match.SchemaMatcher;

/**
 * Generate a schema matching using the {@link PostNASSchemaMatcher}.
 *
 * @author Simon Templer
 */
public class PostNASMatchSchemas extends MatchSchemasCommand {

  @Override
  protected SchemaMatcher createMatcher() {
    return new PostNASSchemaMatcher();
  }

  @Override
  public String getShortDescription() {
    return "Generate a mapping from a reference schema (AAA XSD) to a target schema (PostNAS) based on a fixed set of pre-defined rules";
  }

}
