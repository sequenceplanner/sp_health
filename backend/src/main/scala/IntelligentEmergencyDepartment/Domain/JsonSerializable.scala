package IntelligentEmergencyDepartment.Domain

import play.api.libs.json.JsValue

trait JsonSerializable {
  def asJson():JsValue

}
