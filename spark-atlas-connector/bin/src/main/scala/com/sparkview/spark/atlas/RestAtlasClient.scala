/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.sparkview.spark.atlas

import java.util

import scala.collection.JavaConverters._
import com.sun.jersey.core.util.MultivaluedMapImpl
// import org.apache.atlas.AtlasClientV2
import org.apache.atlas.model.SearchFilter
import org.apache.atlas.model.instance.AtlasEntity
import org.apache.atlas.model.instance.AtlasEntity.{AtlasEntitiesWithExtInfo, AtlasEntityWithExtInfo}
import org.apache.atlas.model.typedef.AtlasTypesDef
import com.sparkview.atlas.utils.AuthenticationUtil

// custom java classes
import com.sparkview.atlas.AtlasClientV2

class RestAtlasClient(atlasClientConf: AtlasClientConf) extends AtlasClient {

  private val client = {
    if (!AuthenticationUtil.isKerberosAuthenticationEnabled) {
      // val oauthServicePrincipal = 
      // Array(atlasClientConf.get(AtlasClientConf.CLIENT_USERNAME), atlasClientConf.get(AtlasClientConf.CLIENT_PASSWORD))
  
      new AtlasClientV2(getServerUrl(), getOauthServicePrincipal())
    } else {
      new AtlasClientV2(getServerUrl(): _*) 
    }
  }

  private def getOauthServicePrincipal(): Array[String] = {
    val tenantID: String = sys.env.getOrElse("TENANT_ID", "")
    val clientID: String = sys.env.getOrElse("CLIENT_ID", "")
    val clientSecret: String = sys.env.getOrElse("CLIENT_SECRET", "")
    return Array(tenantID, clientID, clientSecret)
  }

  // TODO get it from environment variable
  private def getServerUrl(): Array[String] = {
    val serverURL: String = sys.env.getOrElse("ATLAS_REST_ADDRESS", "")
    return Array(serverURL)

    // atlasClientConf.getUrl(AtlasClientConf.ATLAS_REST_ENDPOINT.key) match {
    //   case a: util.ArrayList[_] => a.toArray().map(b => b.toString)
    //   case s: String => Array(s)
    //   case _: Throwable => throw new IllegalArgumentException(s"Fail to get atlas.rest.address")
    // }
  }

  override def createAtlasTypeDefs(typeDefs: AtlasTypesDef): Unit = {
    client.createAtlasTypeDefs(typeDefs)
  }

  override def getAtlasTypeDefs(searchParams: MultivaluedMapImpl): AtlasTypesDef = {
    val searchFilter = new SearchFilter(searchParams)
    client.getAllTypeDefs(searchFilter)
  }

  override def updateAtlasTypeDefs(typeDefs: AtlasTypesDef): Unit = {
    client.updateAtlasTypeDefs(typeDefs)
  }

  override protected def doCreateEntities(entities: Seq[AtlasEntity]): Unit = {
    val entitesWithExtInfo = new AtlasEntitiesWithExtInfo()
    entities.foreach(entitesWithExtInfo.addEntity)
    val response = client.createEntities(entitesWithExtInfo)
    try {
      logInfo(s"Entities ${response.getCreatedEntities.asScala.map(_.getGuid).mkString(", ")} " +
        s"created")
    } catch {
      case _: Throwable => throw new IllegalStateException(s"Fail to get create entities")
    }
  }

  override protected def doDeleteEntityWithUniqueAttr(
      entityType: String,
      attribute: String): Unit = {
    client.deleteEntityByAttribute(entityType,
      Map(org.apache.atlas.AtlasClient.REFERENCEABLE_ATTRIBUTE_NAME -> attribute).asJava)
  }

  override protected def doUpdateEntityWithUniqueAttr(
      entityType: String,
      attribute: String,
      entity: AtlasEntity): Unit = {
    client.updateEntityByAttribute(
      entityType,
      Map(org.apache.atlas.AtlasClient.REFERENCEABLE_ATTRIBUTE_NAME -> attribute).asJava,
      new AtlasEntityWithExtInfo(entity))
  }
}
