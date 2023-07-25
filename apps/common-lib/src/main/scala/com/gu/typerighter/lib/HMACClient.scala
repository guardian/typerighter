package com.gu.typerighter.lib

import com.gu.hmac.HMACHeaders
import com.gu.pandahmac.HMACHeaderNames

import java.net.URI

class HMACClient(stage: String, secretKey: String) extends HMACHeaders {
  def secret: String = secretKey

  def getHMACHeaders(uri: String): List[(String, String)] = {
    val headerValues = createHMACHeaderValues(new URI(uri))
    List(
      HMACHeaderNames.dateKey -> headerValues.date,
      HMACHeaderNames.hmacKey -> headerValues.token,
      HMACHeaderNames.serviceNameKey -> s"rule-manager-${stage}"
    )
  }
}
