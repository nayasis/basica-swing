package io.github.nayasis.kotlin.swing.app.di

import io.github.nayasis.kotlin.swing.app.di.scan.ScanInjectService
import io.github.nayasis.kotlin.swing.app.di.scan.ScanNamedInjectService
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlin.test.Test

class SimpleDiContainerTest {

    @Test
    fun `basic set and get`() {
        val container = SimpleDiContainer()
        val service = TestService()
        
        container.set(service)
        val retrieved = container.get(TestService::class)
        
        retrieved shouldBe service
    }

    @Test
    fun `set with custom name`() {
        val container = SimpleDiContainer()
        val service1 = TestService("service1")
        val service2 = TestService("service2")
        
        container.set(bean = service1, beanName = "service1")
        container.set(bean = service2, beanName = "service2")
        
        container.get(TestService::class, "service1")?.name shouldBe "service1"
        container.get(TestService::class, "service2")?.name shouldBe "service2"
    }

    @Test
    fun `set with KClass creates instance`() {
        val container = SimpleDiContainer()
        
        container.set(TestService::class)
        val retrieved = container.get(TestService::class)
        
        retrieved shouldNotBe null
        retrieved?.name shouldBe "default"
    }

    @Test
    fun `set multiple beans at once`() {
        val container = SimpleDiContainer()
        
        container.set(TestService("a"), TestService("b"), TestService("c"))
        
        // When multiple beans of same type are set without names, followers could not be created.
        container.get(TestService::class)?.name shouldBe "a"
    }

    @Test
    fun `'get' returns null when not found`() {
        val container = SimpleDiContainer()
        container.get(TestService::class) shouldBe null
    }

    @Test
    fun `remove by class`() {
        val container = SimpleDiContainer()
        val service = TestService()
        
        container.set(service)
        container.get(TestService::class) shouldBe service
        
        container.remove(TestService::class)

        container.get(TestService::class) shouldBe null
    }

    @Test
    fun `remove by name`() {
        val container = SimpleDiContainer()
        val service1 = TestService("service1")
        val service2 = TestService("service2")
        
        container.set(bean = service1, beanName = "service1")
        container.set(bean = service2, beanName = "service2")
        
        container.remove(TestService::class, "service1")
        
        container.get(TestService::class, "service2")?.name shouldBe "service2"
        container.get(TestService::class, "service1")?.name shouldBe null
    }

    @Test
    fun `Inject annotation creates instance automatically`() {
        val container = SimpleDiContainer()
        
        val service = container.create(InjectTestService::class)
        
        service shouldNotBe null
        service.name shouldBe "default"
    }

    @Test
    fun `Inject with name parameter`() {
        val container = SimpleDiContainer()
        
        val service1 = container.create(InjectWithNameService::class)
        val service2 = container.create(InjectWithNameService::class)
        
        service1 shouldBe service2 // Should be same instance
        service1.name shouldBe "custom"
    }

    @Test
    fun `dependency injection works`() {
        val container = SimpleDiContainer()
        
        val dependent = container.create(DependentService::class)
        
        dependent shouldNotBe null
        dependent.dependency shouldNotBe null
        dependent.dependency.name shouldBe "default"
    }

    @Test
    fun `circular dependency detection`() {
        val container = SimpleDiContainer()
        
        val exception = shouldThrow<IllegalStateException> {
            container.create(CircularServiceA::class)
        }
        exception.message shouldNotBe null
        exception.message!!.contains("Circular dependency detected") shouldBe true
        exception.message!!.contains("CircularServiceA") shouldBe true
        exception.message!!.contains("CircularServiceB") shouldBe true
    }

    @Test
    fun `multiple dependencies injection`() {
        val container = SimpleDiContainer()
        
        val service = container.create(MultiDependentService::class)
        
        service shouldNotBe null
        service.dep1 shouldNotBe null
        service.dep2 shouldNotBe null
    }

    @Test
    fun `Inject annotation with empty name uses class name`() {
        val container = SimpleDiContainer()
        
        val service = container.create(InjectTestService::class)
        
        // Should be retrievable by class name (stored with class simple name)
        val retrieved = container.get(InjectTestService::class, "InjectTestService")
        retrieved shouldBe service
    }

    @Test
    fun `scan from parent package automatically creates instances`() {
        val container = SimpleDiContainer()
        
        // Scan the test package
        container.scanPackages("io.github.nayasis.kotlin.swing.app.di.scan")
        
        // After scanning, @Inject annotated classes should be available
        val service = container.get(ScanInjectService::class)
        service shouldNotBe null
        service?.name shouldBe "default"
        
        val withNameService = container.get(ScanNamedInjectService::class)
        withNameService shouldNotBe null
        withNameService?.name shouldBe "custom"
    }

    @Test
    fun `scan packages automatically creates instances`() {
        val container = SimpleDiContainer()

        // Scan the test package
        container.scanPackages("io.github.nayasis.kotlin.swing.app.di.scan")

        // After scanning, @Inject annotated classes should be available
        val service = container.get(ScanInjectService::class)
        service shouldNotBe null
        service?.name shouldBe "default"

        val withNameService = container.get(ScanNamedInjectService::class)
        withNameService shouldNotBe null
        withNameService?.name shouldBe "custom"
    }

    @Test
    fun `scan packages handles multiple packages`() {
        val container = SimpleDiContainer()
        
        // Scan multiple packages
        container.scanPackages(
            "io.github.nayasis.kotlin.swing.app.di.scan",
            "io.github.nayasis.kotlin.swing.app.di.scan"
        )
        
        // Should be able to get instances
        val service = container.get(ScanInjectService::class)
        service shouldNotBe null
    }

    @Test
    fun `scan packages throws when bean creation fails`() {
        val container = SimpleDiContainer()

        shouldThrow<IllegalStateException> {
            container.scanPackages("io.github.nayasis.kotlin.swing.app.di")
        }
    }
}

// Test classes

@Inject
class TestService {
    val name: String
    
    constructor(name: String = "default") {
        this.name = name
    }
}

@Inject
class InjectTestService {
    val name: String
    
    constructor(name: String = "default") {
        this.name = name
    }
}

@Inject(name = "custom")
class InjectWithNameService {
    val name: String
    
    constructor(name: String = "custom") {
        this.name = name
    }
}

@Inject
class DependencyService {
    val name: String
    
    constructor(name: String = "default") {
        this.name = name
    }
}

@Inject
class DependentService(val dependency: DependencyService)

@Inject
class CircularServiceA(val serviceB: CircularServiceB)

@Inject
class CircularServiceB(val serviceA: CircularServiceA)

@Inject
class Dependency1 {
    val name: String
    
    constructor(name: String = "dep1") {
        this.name = name
    }
}

@Inject
class Dependency2 {
    val name: String
    
    constructor(name: String = "dep2") {
        this.name = name
    }
}

@Inject
class MultiDependentService(val dep1: Dependency1, val dep2: Dependency2)

