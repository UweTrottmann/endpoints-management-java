/*
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
/*
 * This code was generated by https://github.com/google/apis-client-generator/
 * Modify at your own risk.
 */

package com.google.api.services.servicecontrol.v1.model;

/**
 * `QuotaGroup` defines a set of quota limits to enforce.
 *
 * <p> This is the Java data model class that specifies how to parse/serialize into the JSON that is
 * transmitted over HTTP when working with the Google Service Control API. For a detailed
 * explanation see:
 * <a href="https://developers.google.com/api-client-library/java/google-http-java-client/json">https://developers.google.com/api-client-library/java/google-http-java-client/json</a>
 * </p>
 *
 * @author Google, Inc.
 */
@SuppressWarnings("javadoc")
public final class QuotaGroup extends com.google.api.client.json.GenericJson {

  /**
   * Indicates if the quota limits defined in this quota group apply to consumers who have active
   * billing. Quota limits defined in billable groups will be applied only to consumers who have
   * active billing. The amount of tokens consumed from billable quota group will also be reported
   * for billing. Quota limits defined in non-billable groups will be applied only to consumers who
   * have no active billing.
   * The value may be {@code null}.
   */
  @com.google.api.client.util.Key
  private java.lang.Boolean billable;

  /**
   * User-visible description of this quota group.
   * The value may be {@code null}.
   */
  @com.google.api.client.util.Key
  private java.lang.String description;

  /**
   * Quota limits to be enforced when this quota group is used. A request must satisfy all the
   * limits in a group for it to be permitted.
   * The value may be {@code null}.
   */
  @com.google.api.client.util.Key
  private java.util.List<QuotaLimit> limits;

  static {
    // hack to force ProGuard to consider QuotaLimit used, since otherwise it would be stripped out
    // see https://github.com/google/google-api-java-client/issues/543
    com.google.api.client.util.Data.nullOf(QuotaLimit.class);
  }

  /**
   * Name of this quota group. Must be unique within the service.
   *
   * Quota group name is used as part of the id for quota limits. Once the quota group has been put
   * into use, the name of the quota group should be immutable.
   * The value may be {@code null}.
   */
  @com.google.api.client.util.Key
  private java.lang.String name;

  /**
   * Indicates if the quota limits defined in this quota group apply to consumers who have active
   * billing. Quota limits defined in billable groups will be applied only to consumers who have
   * active billing. The amount of tokens consumed from billable quota group will also be reported
   * for billing. Quota limits defined in non-billable groups will be applied only to consumers who
   * have no active billing.
   * @return value or {@code null} for none
   */
  public java.lang.Boolean getBillable() {
    return billable;
  }

  /**
   * Indicates if the quota limits defined in this quota group apply to consumers who have active
   * billing. Quota limits defined in billable groups will be applied only to consumers who have
   * active billing. The amount of tokens consumed from billable quota group will also be reported
   * for billing. Quota limits defined in non-billable groups will be applied only to consumers who
   * have no active billing.
   * @param billable billable or {@code null} for none
   */
  public QuotaGroup setBillable(java.lang.Boolean billable) {
    this.billable = billable;
    return this;
  }

  /**
   * User-visible description of this quota group.
   * @return value or {@code null} for none
   */
  public java.lang.String getDescription() {
    return description;
  }

  /**
   * User-visible description of this quota group.
   * @param description description or {@code null} for none
   */
  public QuotaGroup setDescription(java.lang.String description) {
    this.description = description;
    return this;
  }

  /**
   * Quota limits to be enforced when this quota group is used. A request must satisfy all the
   * limits in a group for it to be permitted.
   * @return value or {@code null} for none
   */
  public java.util.List<QuotaLimit> getLimits() {
    return limits;
  }

  /**
   * Quota limits to be enforced when this quota group is used. A request must satisfy all the
   * limits in a group for it to be permitted.
   * @param limits limits or {@code null} for none
   */
  public QuotaGroup setLimits(java.util.List<QuotaLimit> limits) {
    this.limits = limits;
    return this;
  }

  /**
   * Name of this quota group. Must be unique within the service.
   *
   * Quota group name is used as part of the id for quota limits. Once the quota group has been put
   * into use, the name of the quota group should be immutable.
   * @return value or {@code null} for none
   */
  public java.lang.String getName() {
    return name;
  }

  /**
   * Name of this quota group. Must be unique within the service.
   *
   * Quota group name is used as part of the id for quota limits. Once the quota group has been put
   * into use, the name of the quota group should be immutable.
   * @param name name or {@code null} for none
   */
  public QuotaGroup setName(java.lang.String name) {
    this.name = name;
    return this;
  }

  @Override
  public QuotaGroup set(String fieldName, Object value) {
    return (QuotaGroup) super.set(fieldName, value);
  }

  @Override
  public QuotaGroup clone() {
    return (QuotaGroup) super.clone();
  }

}