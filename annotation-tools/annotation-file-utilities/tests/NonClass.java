public class NonClass {
  interface A { void m(Object p); }
  enum B { ONE; void m() { Object l; } }
  @interface C { String value() default "Ha!"; }
  enum E {
    D((String) new String());
    E(String s) { }
  }
}
