package com.rizkysiregar.narrativ.testing.fakes

import com.rizkysiregar.narrativ.i18n.AppStrings

/** Returns "res:<id>" (and the arguments), so a test asserts on which message was chosen without reading resources. */
class FakeAppStrings : AppStrings {
	override fun get(id: Int, vararg args: Any): String =
		if (args.isEmpty()) "res:$id" else "res:$id:${args.joinToString(",")}"
}
