package com.rizkysiregar.feedzmore.constants

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 18/11/25
 * Description: Software Engineer
 */

object Constants {

	// env url
	//const val domain = "http://10.0.2.2:9091/" // local
	const val domain = "http://192.168.1.50:9091/"

	const val BASE_URL = "${domain}api/v1/";
	const val BASE_IMAGE_URL = "${domain}api/v1/res/photo?fileName=";
	const val MAIN_GRAPH = "main_graph"
	const val HOME_GRAPH = "home_graph"
}