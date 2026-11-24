package com.rizkysiregar.narrativ.di

import com.rizkysiregar.narrativ.data.remote.DeviceSyncScheduler
import com.rizkysiregar.narrativ.data.remote.WorkDeviceSyncScheduler
import com.rizkysiregar.narrativ.data.repository.ApiDeviceRegistrar
import com.rizkysiregar.narrativ.data.repository.DeviceRegistrar
import com.rizkysiregar.narrativ.data.session.CredentialManagerGoogleIdTokenProvider
import com.rizkysiregar.narrativ.data.session.ApiRemoteSessionCleaner
import com.rizkysiregar.narrativ.data.session.FirebasePushTokenProvider
import com.rizkysiregar.narrativ.data.session.PushTokenProvider
import com.rizkysiregar.narrativ.data.session.GoogleIdTokenProvider
import com.rizkysiregar.narrativ.data.session.RemoteSessionCleaner
import com.rizkysiregar.narrativ.domain.post.AndroidMediaInspector
import com.rizkysiregar.narrativ.domain.post.FileMediaPreparationUseCase
import com.rizkysiregar.narrativ.domain.post.MediaInspector
import com.rizkysiregar.narrativ.domain.post.MediaPreparationUseCase
import com.rizkysiregar.narrativ.domain.post.UploadManager
import com.rizkysiregar.narrativ.media.BitmapImageCompressor
import com.rizkysiregar.narrativ.media.ImageCompressor
import com.rizkysiregar.narrativ.media.Media3VideoCompressor
import com.rizkysiregar.narrativ.media.VideoCompressor
import com.rizkysiregar.narrativ.domain.post.WorkUploadManager
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Services that talk to the system or to Google, behind interfaces so view model tests can fake them. */
@Module
@InstallIn(SingletonComponent::class)
abstract class ServicesModule {

	@Binds
	@Singleton
	abstract fun bindDeviceSyncScheduler(impl: WorkDeviceSyncScheduler): DeviceSyncScheduler

	@Binds
	@Singleton
	abstract fun bindRemoteSessionCleaner(impl: ApiRemoteSessionCleaner): RemoteSessionCleaner

	@Binds
	@Singleton
	abstract fun bindGoogleIdTokenProvider(impl: CredentialManagerGoogleIdTokenProvider): GoogleIdTokenProvider

	@Binds
	abstract fun bindMediaInspector(impl: AndroidMediaInspector): MediaInspector

	@Binds
	@Singleton
	abstract fun bindMediaPreparation(impl: FileMediaPreparationUseCase): MediaPreparationUseCase

	@Binds
	@Singleton
	abstract fun bindUploadManager(impl: WorkUploadManager): UploadManager

	@Binds
	@Singleton
	abstract fun bindPushTokenProvider(impl: FirebasePushTokenProvider): PushTokenProvider

	@Binds
	@Singleton
	abstract fun bindChatAvatars(impl: com.rizkysiregar.narrativ.chat.NarrativChatAvatars): com.rizkysiregar.narrativ.chat.ChatAvatars

	@Binds
	@Singleton
	abstract fun bindChatSound(impl: com.rizkysiregar.narrativ.notification.ChannelChatSound): com.rizkysiregar.narrativ.notification.ChatSound

	@Binds
	@Singleton
	abstract fun bindChatSession(impl: com.rizkysiregar.narrativ.chat.MarpokatChatSession): com.rizkysiregar.narrativ.chat.ChatSession

	@Binds
	@Singleton
	abstract fun bindChatAttachmentReader(impl: com.rizkysiregar.narrativ.chat.ContentResolverAttachmentReader): com.rizkysiregar.narrativ.chat.ChatAttachmentReader

	@Binds
	@Singleton
	abstract fun bindChatClientFactory(impl: com.rizkysiregar.narrativ.chat.AndroidChatClientFactory): com.rizkysiregar.narrativ.chat.ChatClientFactory

	@Binds
	@Singleton
	abstract fun bindDeviceRegistrar(impl: ApiDeviceRegistrar): DeviceRegistrar

	@Binds
	@Singleton
	abstract fun bindImageCompressor(impl: BitmapImageCompressor): ImageCompressor

	@Binds
	@Singleton
	abstract fun bindVideoCompressor(impl: Media3VideoCompressor): VideoCompressor
}
