package IntelligentEmergencyDepartment.Util


import scalaj.http.{Http, HttpOptions, HttpResponse}

import play.api.libs.json.{JsArray, JsObject, JsValue, Json}


import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Try

/**
 * Responsible for posting json to urls
 */
object PostManager {

  def postToPowerBi(url: String, data: JsObject): Future[Try[HttpResponse[String]]] = {
    Future {
      Try {
        Http(
          url
        ).postData(data.toString()
        )
          .header("Content-Type", "application/json")
          .header("Charset", "UTF-8")
          .option(HttpOptions.readTimeout(10000)).asString

      }
    }
  }


  def postToPowerBi(url: String, data: List[JsValue]): Future[Try[HttpResponse[String]]] = { //Unlike the other post function this one sends a list of things
    Future {
      Try {
        Http(
          url
        ).postData(createRowArray(JsArray(data)).toString() //converts the list to a json array, then a json object and lastly a string
        )
          .header("Content-Type", "application/json")
          .header("Charset", "UTF-8")
          .option(HttpOptions.readTimeout(4000)).asString
      }
    }
    //println("Did the push work? " + result.code) //Print the code of the result. if the code is 200, the push worked*/
  }



  def createRowArray(jsArray: JsArray): JsObject = {
    Json.obj("rows" -> jsArray) //Converts the json array to a json object where the array is labeled "rows"
  }

}
