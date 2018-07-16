package cloud.localstack;

// import cloud.localstack.LocalStack;

public class OutpaceUtil {
  public static void setup() {
    Localstack.INSTANCE.setupInfrastructure();
  }

  public static void teardown() {
    Localstack.teardownInfrastructure();
  }
}
