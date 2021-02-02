package de.eternalwings.rekurrence.support

import org.junit.jupiter.api.extension.Extension
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.TestTemplateInvocationContext
import org.junit.jupiter.api.extension.TestTemplateInvocationContextProvider
import java.io.InputStreamReader
import java.util.stream.Stream

class RecurringRuleRFCTestInvocationContextProvider : TestTemplateInvocationContextProvider {
    override fun supportsTestTemplate(context: ExtensionContext?): Boolean = true

    override fun provideTestTemplateInvocationContexts(context: ExtensionContext?): Stream<TestTemplateInvocationContext> {
        val file = javaClass.classLoader.getResourceAsStream("rfc-test-cases.txt") ?: throw IllegalStateException()
        val content = InputStreamReader(file).use { it.readText() }
        return RFCTestCaseParser.parse(content).stream().map { rfcContext(it) }
    }

    private fun rfcContext(rfcTestCase: RFCTestCase): TestTemplateInvocationContext {
        return object : TestTemplateInvocationContext {
            override fun getDisplayName(invocationIndex: Int): String {
                return rfcTestCase.name
            }

            override fun getAdditionalExtensions(): MutableList<Extension> {
                return mutableListOf(
                    GenericTypedParameterResolver(rfcTestCase)
                )
            }
        }
    }

}
