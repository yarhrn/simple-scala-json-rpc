package jsonrpc.examples

object Common {

  /** Flat-string wrapper. Serializes as the bare String, not as `{"id": "..."}`.
    * The play-json [[play.api.libs.json.Format]] lives in [[PlayJsonFormats]].
    */
  case class RamielId(id: String)
}
