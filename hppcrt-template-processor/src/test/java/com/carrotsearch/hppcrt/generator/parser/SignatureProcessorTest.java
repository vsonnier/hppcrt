package com.carrotsearch.hppcrt.generator.parser;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.logging.Level;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.carrotsearch.hppcrt.generator.TemplateOptions;
import com.carrotsearch.hppcrt.generator.Type;
import com.carrotsearch.randomizedtesting.RandomizedRunner;
import com.carrotsearch.randomizedtesting.annotations.ThreadLeakLingering;

@RunWith(RandomizedRunner.class)
//To be able to display GUI
@ThreadLeakLingering(linger = Integer.MAX_VALUE)
public class SignatureProcessorTest
{
    private static final String TEST_CASE_RESOURCE_PATH = "test_cases/";

    private boolean displayParseTree = false;

    @Before
    public void setUp() {

        // set to true to launch the GUI for the tests.
        this.displayParseTree = false;
    }

    @Test
    public void testClassKV() throws IOException {
        final SignatureProcessor sp = new SignatureProcessor("public class KTypeVTypeClass<KType, VType> {}");
        check(Type.INT, Type.LONG, sp, "public class IntLongClass {}");
        check(Type.INT, Type.GENERIC, sp, "public class IntObjectClass<VType> {}");
        check(Type.GENERIC, Type.LONG, sp, "public class ObjectLongClass<KType> {}");
        check(Type.GENERIC, Type.GENERIC, sp, "public class ObjectObjectClass<KType, VType> {}");
    }

    @Test
    public void testClassVK_SignatureReversed() throws IOException {
        final SignatureProcessor sp = new SignatureProcessor("public class KTypeVTypeClass<VType, KType> {}");
        check(Type.INT, Type.LONG, sp, "public class LongIntClass {}");
        check(Type.INT, Type.GENERIC, sp, "public class ObjectIntClass<VType> {}");
        check(Type.GENERIC, Type.LONG, sp, "public class LongObjectClass<KType> {}");
        check(Type.GENERIC, Type.GENERIC, sp, "public class ObjectObjectClass<VType, KType> {}");
    }

    @Test
    public void testClassK() throws IOException {
        final SignatureProcessor sp = new SignatureProcessor("public class KTypeClass<KType> {}");
        check(Type.INT, sp, "public class IntClass {}");
        check(Type.GENERIC, sp, "public class ObjectClass<KType> {}");
    }

    @Test
    public void testClassExtendsNonTemplate() throws IOException {
        final SignatureProcessor sp = new SignatureProcessor("public class KTypeVTypeClass<KType, VType> extends SuperClass {}");
        check(Type.INT, Type.LONG, sp, "public class IntLongClass extends SuperClass {}");
        check(Type.INT, Type.GENERIC, sp, "public class IntObjectClass<VType> extends SuperClass {}");
        check(Type.GENERIC, Type.LONG, sp, "public class ObjectLongClass<KType> extends SuperClass {}");
        check(Type.GENERIC, Type.GENERIC, sp, "public class ObjectObjectClass<KType, VType> extends SuperClass {}");
    }

    @Test
    public void testClassExtendsTemplate() throws IOException {
        final SignatureProcessor sp = new SignatureProcessor(
                "public class KTypeVTypeClass<KType, VType> extends KTypeVTypeSuperClass<KType, VType> {}");
        check(Type.INT, Type.LONG, sp, "public class IntLongClass extends IntLongSuperClass {}");
        check(Type.INT, Type.GENERIC, sp, "public class IntObjectClass<VType> extends IntObjectSuperClass<VType> {}");
        check(Type.GENERIC, Type.LONG, sp, "public class ObjectLongClass<KType> extends ObjectLongSuperClass<KType> {}");
        check(Type.GENERIC, Type.GENERIC, sp, "public class ObjectObjectClass<KType, VType> extends ObjectObjectSuperClass<KType, VType> {}");
    }

    @Test
    public void testClassImplementsTemplate() throws IOException {
        final SignatureProcessor sp = new SignatureProcessor(
                "public class KTypeVTypeClass<KType, VType> " +
                        " extends     KTypeVTypeSuperClass<KType, VType>" +
                " implements  KTypeVTypeInterface<KType, VType> {}");

        check(Type.INT, Type.LONG, sp, "public class IntLongClass extends IntLongSuperClass implements IntLongInterface {}");
        check(Type.INT, Type.GENERIC, sp, "public class IntObjectClass<VType> extends IntObjectSuperClass<VType> implements IntObjectInterface<VType> {}");
        check(Type.GENERIC, Type.LONG, sp, "public class ObjectLongClass<KType> extends ObjectLongSuperClass<KType> implements ObjectLongInterface<KType> {}");
        check(Type.GENERIC, Type.GENERIC, sp, "public class ObjectObjectClass<KType, VType> extends ObjectObjectSuperClass<KType, VType> implements ObjectObjectInterface<KType, VType> {}");
    }

    @Test
    public void testInterfaceKV() throws IOException {
        final SignatureProcessor sp = new SignatureProcessor(
                "public interface KTypeVTypeInterface<KType, VType> " +
                "         extends KTypeVTypeSuper<KType, VType> {}");

        check(Type.INT, Type.LONG, sp, "public interface IntLongInterface extends IntLongSuper {}");
        check(Type.INT, Type.GENERIC, sp, "public interface IntObjectInterface<VType> extends IntObjectSuper<VType> {}");
        check(Type.GENERIC, Type.LONG, sp, "public interface ObjectLongInterface<KType> extends ObjectLongSuper<KType> {}");
        check(Type.GENERIC, Type.GENERIC, sp, "public interface ObjectObjectInterface<KType, VType> extends ObjectObjectSuper<KType, VType> {}");
    }

    @Test
    public void testImportDeclarations() throws IOException {
        final SignatureProcessor sp = new SignatureProcessor(
                "import foo.KTypeVTypeClass; class Foo {}");

        check(Type.INT, Type.LONG, sp, "import foo.IntLongClass; class Foo {}");
        check(Type.INT, Type.GENERIC, sp, "import foo.IntObjectClass; class Foo {}");
        check(Type.GENERIC, Type.LONG, sp, "import foo.ObjectLongClass; class Foo {}");
        check(Type.GENERIC, Type.GENERIC, sp, "import foo.ObjectObjectClass; class Foo {}");
    }

    @Test
    public void testFieldDeclaration() throws IOException {
        final SignatureProcessor sp = new SignatureProcessor(
                "class KTypeFoo<KType> { KType foo; KType [] foo2; }");

        check(Type.FLOAT, sp, "class FloatFoo { float foo; float [] foo2; }");
        check(Type.GENERIC, sp, "class ObjectFoo<KType> { KType foo; KType [] foo2; }");
    }

    @Test
    public void testClassConstructor() throws IOException {

        final SignatureProcessor sp = new SignatureProcessor(
                "class KTypeVTypeFoo<KType, VType> { public KTypeVTypeFoo(KType k, VType v) {} }");

        check(Type.FLOAT, Type.DOUBLE, sp, "class FloatDoubleFoo { public FloatDoubleFoo(float k, double v) {} }");
        check(Type.FLOAT, Type.GENERIC, sp, "class FloatObjectFoo<VType> { public FloatObjectFoo(float k, VType v) {} }");
        check(Type.GENERIC, Type.FLOAT, sp, "class ObjectFloatFoo<KType> { public ObjectFloatFoo(KType k, float v) {} }");
        check(Type.GENERIC, Type.GENERIC, sp, "class ObjectObjectFoo<KType, VType> { public ObjectObjectFoo(KType k, VType v) {} }");
    }

    @Test
    public void testThisReference() throws IOException {
        final SignatureProcessor sp = new SignatureProcessor(
                "class KTypeVTypeFoo<KType, VType> { public void foo() { KTypeVTypeFoo.this.foo(); } }");

        check(Type.FLOAT, Type.DOUBLE, sp, "class FloatDoubleFoo { public void foo() { FloatDoubleFoo.this.foo(); } }");
    }

    @Test
    public void testNewClassDiamond() throws IOException {

        final SignatureProcessor sp = new SignatureProcessor(
                "class KTypeVTypeFoo<KType, VType> { public void foo() { new KTypeVTypeFoo<>(); } }");

        check(Type.FLOAT, Type.DOUBLE, sp, "class FloatDoubleFoo { public void foo() { new FloatDoubleFoo(); } }");
        check(Type.GENERIC, Type.DOUBLE, sp, "class ObjectDoubleFoo<KType> { public void foo() { new ObjectDoubleFoo<>(); } }");
    }

    @Test
    public void testNewClass() throws IOException {

        final SignatureProcessor sp = new SignatureProcessor(
                "class KTypeVTypeFoo<KType, VType> { public void foo() { new KTypeVTypeFoo<KType, VType>(); } }");

        check(Type.FLOAT, Type.DOUBLE, sp, "class FloatDoubleFoo { public void foo() { new FloatDoubleFoo(); } }");
        check(Type.GENERIC, Type.DOUBLE, sp, "class ObjectDoubleFoo<KType> { public void foo() { new ObjectDoubleFoo<KType>(); } }");
    }

    @Test
    public void testStaticGenericMethod() throws IOException {
        final SignatureProcessor sp = new SignatureProcessor(
                "class KTypeVTypeFoo<KType, VType> { static <KType, VType> KTypeVTypeFoo foo(KType[] k, VType[] v) {} }");

        check(Type.FLOAT, Type.DOUBLE, sp, "class FloatDoubleFoo { static FloatDoubleFoo foo(float[] k, double[] v) {} }");
        check(Type.GENERIC, Type.DOUBLE, sp, "class ObjectDoubleFoo<KType> { static <KType> ObjectDoubleFoo foo(KType[] k, double[] v) {} }");
    }

    @Test
    public void testWildcardBound() throws IOException {

        final SignatureProcessor sp = new SignatureProcessor(
                "class KTypeFoo<KType> { void bar(KTypeFoo<?> other) {} }");

        check(Type.FLOAT, sp, "class FloatFoo { void bar(FloatFoo other) {} }");
        check(Type.GENERIC, sp, "class ObjectFoo<KType> { void bar(ObjectFoo<?> other) {} }");
    }

    @Test
    public void testGenericNamedTypeBound() throws IOException {
        final SignatureProcessor sp = new SignatureProcessor(
                "class KTypeFoo<KType> { public <T extends KTypeBar<? super KType>> T forEach(T v) { throw new R(); } }");

        check(Type.FLOAT, sp, "class FloatFoo         { public <T extends FloatBar> T forEach(T v) { throw new R(); } }");
        check(Type.GENERIC, sp, "class ObjectFoo<KType> { public <T extends ObjectBar<? super KType>> T forEach(T v) { throw new R(); } }");
    }

    @Test
    public void testNotGenericNamedTypeBound() throws IOException {

        final SignatureProcessor sp = new SignatureProcessor(
                "class KTypeFoo<KType> { public <T extends IntProcedure> T forEach(T v) { } }");

        check(Type.FLOAT, sp, "class FloatFoo { public <T extends IntProcedure> T forEach(T v) { } }");
        check(Type.GENERIC, sp, "class ObjectFoo<KType> { public <T extends IntProcedure> T forEach(T v) { } }");
    }

    @Test
    public void testBug_ErasesObjectConstructor() throws IOException {
        final SignatureProcessor sp = new SignatureProcessor(
                "class KTypeVTypeFoo<KType, VType> { static { HashSet<Object> values = new HashSet<Object>(); }}");

        check(Type.FLOAT, Type.INT, sp, "class FloatIntFoo { static { HashSet<Object> values = new HashSet<Object>(); }}");
        check(Type.GENERIC, Type.GENERIC, sp, "class ObjectObjectFoo<KType, VType> { static { HashSet<Object> values = new HashSet<Object>(); }}");
    }

    @Test
    public void testBug_ErasesUntemplated() throws IOException {

        final SignatureProcessor sp = new SignatureProcessor(
                "class KTypeFoo<KType> { void foo() { KTypeBar<B> x = new KTypeBar<B>(); } }");

        check(Type.FLOAT, sp, "class FloatFoo { void foo() { ObjectBar<B> x = new ObjectBar<B>(); } }");
        check(Type.GENERIC, sp, "class ObjectFoo<KType> { void foo() { ObjectBar<B> x = new ObjectBar<B>(); } }");
    }

    @Test
    public void testUnboundedUnknownWildcard() throws IOException {

        final SignatureProcessor sp = new SignatureProcessor(
                "class KTypeFoo<KType> { void foo() { boolean bTEST = Bar instanceof TestUnbounded<?>; }}");

        check(Type.FLOAT, sp, "class FloatFoo { void foo() { boolean bTEST = Bar instanceof TestUnbounded<?>; }}");
        check(Type.GENERIC, sp, "class ObjectFoo<KType> { void foo() { boolean bTEST = Bar instanceof TestUnbounded<?>; }}");
    }

    @Test
    public void testBug_EraseNestedPrimitive() throws IOException {
        final SignatureProcessor sp = new SignatureProcessor(
                "class KTypeFoo<KType> { static class Nested<KType> extends KTypeBar<KType> {} }");

        check(Type.FLOAT, sp, "class FloatFoo { static class Nested extends FloatBar {} }");
        check(Type.GENERIC, sp, "class ObjectFoo<KType> { static class Nested<KType> extends ObjectBar<KType> {} }");
    }

    @Test
    public void testJavaDoc_k() throws IOException {
        final SignatureProcessor sp = new SignatureProcessor(
                "/** KTypeFoo KTypes */");

        check(Type.FLOAT, sp, "/** FloatFoo floats */");
        check(Type.GENERIC, sp, "/** ObjectFoo Objects */");
    }


    @Test
    public void testJavaDoc_with_bounds_k() throws IOException {
        final SignatureProcessor sp = new SignatureProcessor("/** KTypeFoo KTypes */");

        check(Type.FLOAT, sp, "/** FloatFoo floats */");
        check(Type.GENERIC, sp, "/** ObjectFoo Objects */");
    }

    @Test
    public void testJavaDoc_kv() throws IOException {
        final SignatureProcessor sp = new SignatureProcessor(
                "/** KTypeFoo KTypes KTypeVTypeFoo VTypes */");

        check(Type.FLOAT, Type.DOUBLE, sp, "/** FloatFoo floats FloatDoubleFoo doubles */");
        check(Type.GENERIC, Type.GENERIC, sp, "/** ObjectFoo Objects ObjectObjectFoo Objects */");
    }

    /**
     * Just like java.util.Arrays
     * @throws IOException
     */
    @Test
    public void testUtilityClassStaticGenericMethods() throws IOException {

        final SignatureProcessor sp = new SignatureProcessor(
                "public final class KTypeArraysClass {  "
                        + "public static <KType> void utilityMethod(final KType[] objectArray, final int startIndex, final int endIndex) {"
                        + "} "
                        + "}");

        check(Type.GENERIC, sp, "public final class ObjectArraysClass {  "
                + "public static <KType> void utilityMethod(final KType[] objectArray, final int startIndex, final int endIndex) {"
                + "} "
                + "}");

        check(Type.INT, sp, "public final class IntArraysClass {  "
                + "public static void utilityMethod(final int[] objectArray, final int startIndex, final int endIndex) {"
                + "} "
                + "}");
    }

    @Test
    public void testMethodCall() throws IOException {

        final SignatureProcessor sp = new SignatureProcessor(
                "public final class Foo {  "
                        + "public static bar() {\n"
                        + "// Static method call alone\n"
                        + "AloneVoidClass.<KType>voidMethod(a,b); "
                        + "AloneVoidClass.<KType>voidMethodOneArgument(a);"
                        + "AloneVoidClass.<KType>voidMethodNoArgument();"
                        + "//This\n"
                        + "this.methodCalledWithThis(c,d);"
                        + "this.methodCalledWithThisOneArgument(one);"
                        + "this.methodCalledWithThisNoArgument();"
                        + "// Not this\n"
                        + "methodCallWithoutThis(c,d);"
                        + "methodCallWithoutThisOneArgument(one);"
                        + "methodCallWithoutThisNoArgument();"
                        + "} "
                        + "}");

        //blocking
        //sp.displayParseTreeGui();

    }

    @Test
    public void testFullClass() throws IOException {

        final String testedPath = "KTypeVTypeClass.test";

        final String expectedLongObjectTest = "LongObjectClass.ok";
        final String expectedLongIntTest = "LongIntClass.ok";
        final String expectedObjectlongTest = "ObjectlongClass.ok";

        compareWithReferenceFile(new TemplateOptions(Type.LONG, Type.GENERIC), testedPath, expectedLongObjectTest);
        compareWithReferenceFile(new TemplateOptions(Type.LONG, Type.INT), testedPath, expectedLongIntTest);
        compareWithReferenceFile(new TemplateOptions(Type.GENERIC, Type.LONG), testedPath, expectedObjectlongTest);
    }

    @Test
    public void testFullClassArrays() throws IOException {

        final String testedPath = "KTypeArraysClass.test";

        final String expectedPathLong = "LongArraysClass.ok";
        final String expectedPathObject = "ObjectArraysClass.ok";

        System.out.println(">>>> Converted to Object: \n\n");
        compareWithReferenceFile(new TemplateOptions(Type.GENERIC, null), testedPath, expectedPathObject);

        System.out.println("\n\n>>>> Converted to long : \n\n");
        compareWithReferenceFile(new TemplateOptions(Type.LONG, null), testedPath, expectedPathLong);
    }

    @Test
    public void testFullClassPartialTemplateSpecialization() throws IOException {

        final String testedPath = "KTypePartialSpecializationClass.test";

        final String expectedPathLong = "LongPartialSpecializationClass.ok";
        final String expectedPathObject = "ObjectPartialSpecializationClass.ok";

        System.out.println(">>>> Converted to Object: \n\n");
        compareWithReferenceFile(new TemplateOptions(Type.GENERIC, null), testedPath, expectedPathObject);

        System.out.println("\n\n>>>> Converted to long : \n\n");
        compareWithReferenceFile(new TemplateOptions(Type.LONG, null), testedPath, expectedPathLong);
    }

    @Test
    public void testIteratorPoolAllocFull() throws IOException {

        final String testedPath = "IteratorPoolAlloc.test";

        final String expectedPathLong = "IteratorPoolAllocLong.ok";
        final String expectedPathObject = "IteratorPoolAllocGeneric.ok";

        System.out.println(">>>> Converted to Object: \n\n");
        compareWithReferenceFile(new TemplateOptions(Type.GENERIC, null), testedPath, expectedPathObject);

        System.out.println("\n\n>>>> Converted to long : \n\n");
        compareWithReferenceFile(new TemplateOptions(Type.LONG, null), testedPath, expectedPathLong);

    }

    @Test
    public void testIteratorPool() throws IOException {

        final String testedPath = "IteratorPool.test";

        final String expectedPathLong = "IteratorPoolLong.ok";
        final String expectedPathObject = "IteratorPoolGeneric.ok";

        System.out.println(">>>> Converted to Object: \n\n");
        compareWithReferenceFile(new TemplateOptions(Type.GENERIC, null), testedPath, expectedPathObject);

        System.out.println("\n\n>>>> Converted to long : \n\n");
        compareWithReferenceFile(new TemplateOptions(Type.LONG, null), testedPath, expectedPathLong);
    }

    /**
     * Not really a test, used for debugging crashs of the parser itself.
     * @throws IOException
     */
    @Test
    public void testCrashParsing() throws IOException {

        final String testedPath = "CrashParser.txt";

        final SignatureProcessor processor = new SignatureProcessor(loadFile(testedPath));

        if (this.displayParseTree) {

            processor.displayParseTreeGui();
        }

        //Compute :
        final String output = processor.process(new TemplateOptions(Type.LONG, Type.GENERIC));

        System.out.println(output);
    }

    @Test
    public void testGenericWithinGenericWithinGeneric() throws IOException {

        //generic within generic within generic
        final SignatureProcessor sp = new SignatureProcessor(
                "class Foo { public void foo() { new KTypeFoo<KTypeInner<KTypeProcecedure<KType>>, A>(); } }");

        check(Type.FLOAT, null, sp, "class Foo { public void foo() { new FloatFoo<FloatInner<FloatProcecedure>, A>(); } }");
        check(Type.GENERIC, null, sp, "class Foo { public void foo() { new ObjectFoo<ObjectInner<ObjectProcecedure<KType>>, A>(); } }");
    }

    @Test
    public void testDoubleGenericWithinGenericWithinGeneric() throws IOException {


        //generic within generic within generic
        final SignatureProcessor sp = new SignatureProcessor(
                "class Foo { public void foo() { new KTypeVTypeFoo<KTypeVTypeInner<KTypeVTypeProcecedure<KType, VType>>, A>(); } }");

        check(Type.FLOAT, Type.GENERIC, sp, "class Foo { public void foo() { new FloatObjectFoo<FloatObjectInner<FloatObjectProcecedure<VType>>, A>(); } }");
        check(Type.GENERIC, Type.INT, sp, "class Foo { public void foo() { new ObjectIntFoo<ObjectIntInner<ObjectIntProcecedure<KType>>, A>(); } }");
        check(Type.GENERIC, Type.GENERIC, sp, "class Foo { public void foo() { new ObjectObjectFoo<ObjectObjectInner<ObjectObjectProcecedure<KType, VType>>, A>(); } }");
        check(Type.FLOAT, Type.INT, sp, "class Foo { public void foo() { new FloatIntFoo<FloatIntInner<FloatIntProcecedure>, A>(); } }");

    }

    @Test
    public void testAbstractClassMethods() throws IOException {

        //generic within generic within generic
        final SignatureProcessor sp = new SignatureProcessor(
                "public abstract class AbstractKTypeVTypeHashMapTest<KType, VType> extends AbstractKTypeVTypeTest<KType, VType> { "
                        + " protected abstract KTypeVTypeMap<KType, VType> createNewMapInstance(final int initialCapacity,"
                        + " final double loadFactor, KTypeHashingStrategy<KType> strategy); } }");

        check(Type.FLOAT, Type.GENERIC, sp,
                "public abstract class AbstractFloatObjectHashMapTest<VType> extends AbstractFloatObjectTest<VType> { "
                        + " protected abstract FloatObjectMap<VType> createNewMapInstance(final int initialCapacity,"
                        + " final double loadFactor, FloatHashingStrategy strategy); } }");

        check(Type.GENERIC, Type.INT, sp,
                "public abstract class AbstractObjectIntHashMapTest<KType> extends AbstractObjectIntTest<KType> { "
                        + " protected abstract ObjectIntMap<KType> createNewMapInstance(final int initialCapacity,"
                        + " final double loadFactor, ObjectHashingStrategy<KType> strategy); } }");

        check(Type.GENERIC, Type.GENERIC, sp,
                "public abstract class AbstractObjectObjectHashMapTest<KType, VType> extends AbstractObjectObjectTest<KType, VType> { "
                        + " protected abstract ObjectObjectMap<KType, VType> createNewMapInstance(final int initialCapacity,"
                        + " final double loadFactor, ObjectHashingStrategy<KType> strategy); } }");

        check(Type.FLOAT, Type.INT, sp,
                "public abstract class AbstractFloatIntHashMapTest extends AbstractFloatIntTest { "
                        + " protected abstract FloatIntMap createNewMapInstance(final int initialCapacity,"
                        + " final double loadFactor, FloatHashingStrategy strategy); } }");

    }

    ///////////////////////
    // Utility methods
    ///////////////////////

    private void check(final Type ktype, final SignatureProcessor processor, final String expected) throws IOException {
        check(new TemplateOptions(ktype, null), processor, expected);
    }

    private void check(final Type ktype, final Type vtype, final SignatureProcessor processor, final String expected) throws IOException {
        check(new TemplateOptions(ktype, vtype), processor, expected);
    }

    private void check(final TemplateOptions templateOptions, final SignatureProcessor processor, final String expected) throws IOException {

        //we want all traces
        templateOptions.verbose = Level.ALL;

        if (this.displayParseTree) {

            processor.displayParseTreeGui();
        }

        final String output = processor.process(templateOptions);

        final String expectedNormalized = expected.trim().replaceAll("\\s+", " ");
        final String outputNormalized = output.trim().replaceAll("\\s+", " ");

        if (!outputNormalized.equals(expectedNormalized)) {

            System.out.println(String.format(Locale.ROOT,
                    "Output:\n'%s'\n" +
                            "Expected:\n'%s'\n", output, expected));

            //this is always false, so trigger the comparison with the original outputs
            //so that is easier to diff them because formatting is kept.
            Assert.assertEquals(expected, output);
        }

        Assert.assertTrue(true);
    }

    private void compareWithReferenceFile(final TemplateOptions templateOptions,
            final String testFileName,
            final String expectedFileName) throws IOException {

        final SignatureProcessor processor = new SignatureProcessor(loadFile(testFileName));

        if (this.displayParseTree) {

            processor.displayParseTreeGui();
        }

        //Compute :
        final String output = processor.process(templateOptions);

        //whitespace is not significant, normalize it
        final String outputNormalized = output.trim().replaceAll("\\s+", " ");

        final String expected = loadFile(expectedFileName);

        //whitespace is not significant, normalize it
        final String expectedNormalized = expected.trim().replaceAll("\\s+", " ");

        //Dump on sysout if not identical
        if (!outputNormalized.equals(expectedNormalized)) {
            System.out.println(String.format(Locale.ROOT,
                    "Wrong output:\n'%s'\n\n", output));

            //this is always false, so trigger the comparison with the original outputs
            //so that is easier to diff them because formatting is kept.
            Assert.assertEquals(expected, output);
        }

        Assert.assertTrue(true);
    }

    /**
     * Load a file fileName as UTF-8 and return its String contents
     * @param fileName
     * @return
     * @throws IOException
     */
    private String loadFile(final String fileName) throws IOException {

        final URL resURL = ClassLoader.getSystemResource(SignatureProcessorTest.TEST_CASE_RESOURCE_PATH + fileName);

        URI resURI = null;
        try {
            resURI = resURL.toURI();
        } catch (final URISyntaxException e) {
            //
            e.printStackTrace();
        }

        final Path resPath = Paths.get(resURI);

        Assert.assertTrue("Not a regular file, or file do not exist : " + resPath.toString(), Files.isRegularFile(resPath));

        return new String(Files.readAllBytes(resPath), StandardCharsets.UTF_8);
    }
}
