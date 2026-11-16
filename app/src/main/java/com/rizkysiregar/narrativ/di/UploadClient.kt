package com.rizkysiregar.narrativ.di

import javax.inject.Qualifier

/**
 * Author: Habib Rizky Almajid Siregar
 * Date: 10/10/26
 * Description: Marks the OkHttp client used for direct-to-storage presigned PUTs
 * (no auth interceptor, long write timeout).
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class UploadClient
