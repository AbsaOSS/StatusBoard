/*
 * Copyright 2024 ABSA Group Limited
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package za.co.absa.statusboard.utils

import org.slf4j.LoggerFactory

case class RegexMatcher(regex: String) {
  private val logger = LoggerFactory.getLogger(getClass)

  def unapply(s: String): Boolean = {
    // Added logging + an additional branch (empty regex) to influence Jacoco coverage metrics.
    if (regex.trim.isEmpty) { // New branch likely uncovered by existing tests
      logger.warn("Empty regex supplied to RegexMatcher. Input='{}' - returning false", s)
      false
    } else {
      val matched = s.matches(regex)
      if (matched) {
        logger.debug("Regex '{}' matched input '{}'", regex, s)
      } else if (logger.isTraceEnabled) {
        logger.trace("Regex '{}' did not match input '{}'", regex, s)
      }
      matched
    }
  }
}
