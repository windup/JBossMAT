/**
 * @author Reid Beckett, Middleware Connections
 * This class contains utilities for generating HTML markup
 *  *
 *    Copyright 2009 Middleware Connections, a division of Cloudware Connections Inc.
 * 
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 * 
 *        http://www.apache.org/licenses/LICENSE-2.0
 * 
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.mwc.mat

import java.text.*

class Markup {
	static def header(markup, relativePath) {
		markup.div(class:"header"){
			div(class:"header_left"){
				a(href:"http://www.jboss.org/mass/MAT.html"){
					img(src:"http://www.jboss.org/theme/images/banners/mass-banner.png", border:0, width:240, height:44)
				}
			}
			div(class:"header_right"){
				markup.yield "WebLogic to JBoss Conversion Estimation Tool - Report generated at ${(new SimpleDateFormat('yyyy/MM/dd HH:mm')).format(new Date())}"
			}
		}
	}



	static def serverNav(markup, server, mbean_types) {
		markup.div(class:"server_nav") {
			b("${server.id}")
			br()
			a(href:"index.html", "All MBeans")
			if(server.weblogicVersion < 9){
				yieldUnescaped("&nbsp;|&nbsp;")
				a(href:"applications.html", "Applications")
			}
			yieldUnescaped("&nbsp;|&nbsp;")
			mbean_types.each {
				a(href:"${it.label.replaceAll(' ','').toLowerCase()}.html", it.label)
				yieldUnescaped("&nbsp;|&nbsp;")
			}
		}
	}
	
	static def mbean(markup, mbean, i){
		markup.div(id:"header_${i}", class:"mbean_header"){
			a(href:"javascript:void(0);", onclick:"toggle('data_${i}');", "+")
			yield("${mbean.Type}/${mbean.Name}")
		}
		markup.div(id: "data_${i}", style:"display:none;", class:"mbean_data_wrapper"){
			mbean.children().each { child ->
				div(class:"mbean_data"){
					div(class:"mbean_key", child.name())
					div(class:"mbean_value", child.text())
				}
			}
		}
		
	}

}
