package test;

/* In case you need to run the main function in the Test class, use the command
 *		mvn clean package exec:java@test -q
 */

 public class Test {
	Test f;

	static Test public_01() {
		Test a = new Test();
		// a: {new7}
		Test b = new Test();
		// a: {new7}, b: {new9}
		b.f = a;
		// a: {new7}, b: {new9}, new9.f: {new7}
		return a;
	}

	static void public_02(int value) {
		Test a = new Test();
		// a: {new17}
		Test b = new Test();
		// a: {new17}, b: {new19}
		b.f = b;
		// a: {new17}, b: {new19}, new19.f: {new19}
		if(value <= 100) {
			// a: {new17}, b: {new19}, new19.f: {new19}
			b.f = a;
			// a: {new17}, b: {new19}, new19.f: {new17}
		}
		else {
			// a: {new17}, b: {new19}, new19.f: {new19}
			b.f = null;
			// a: {new17}, b: {new19}, new19.f: {null}
		}
		// a: {new17}, b: {new19}, new19.f: {null, new17}
	}

	static void public_03(int value) {
		Test a = new Test();
		// a: {new37}
		Test b = new Test();
		// a: {new37}, b: {new39}
		Test c = new Test();
		// a: {new37}, b: {new39}, c: {new41}
		b.f = c;
		// a: {new37}, b: {new39}, c: {new41}, new39.f: {new41}
		while(value < 100) {
			// a: {new37}, b: {new39, new47}, c: {new41}, new39.f: {new41, new47}, new47.f: {new47}
			b.f = new Test();
			// a: {new37}, b: {new39}, c: {new41}, new39.f: {new47}, new47.f: {new47}
			b = b.f;
			// a: {new37}, b: {new47}, c: {new41}, new39.f: {new47}, new47.f: {new47}
			value += 1;
			// a: {new37}, b: {new47}, c: {new41}, new39.f: {new47}, new47.f: {new47}
		}
		c.f = b.f;
		// a: {new37}, b: {new39}, c: {new41}, new39.f: {new41, new47}, new41.f: {new41, new47}
	}

	@SuppressWarnings("null")
	static void public_04(int value) {
		Test a;
		// a: {null}
		Test b = new Test();
		// a: {null}, b: {new62}
		Test c = new Test();
		// a: {null}, b: {new62}, c: {new64}
		Test d = new Test();
		// a: {null}, b: {new62}, c: {new64}, d: {new66}
		b.f = null;
		// a: {null}, b: {new62}, c: {new64}, d: {new66}, new62.f: {null}
		c.f = d;
		// a: {null}, b: {new62}, c: {new64}, d: {new66}, new62.f: {null}, new64.f: {new66}
		if(value == 100) {
			// a: {null}, b: {new62}, c: {new64}, d: {new66}, new62.f: {null}, new64.f: {new66}
			a = b;
			// a: {new62}, b: {new62}, c: {new64}, d: {new66}, new62.f: {null}, new64.f: {new66}
		}
		else if (value == 200) {
			// a: {null}, b: {new62}, c: {new64}, d: {new66}, new62.f: {null}, new64.f: {new66}
			a = c;
			// a: {new64}, b: {new62}, c: {new64}, d: {new66}, new62.f: {null}, new64.f: {new66}
		}
		else {
			// a: {null}, b: {new62}, c: {new64}, d: {new66}, new62.f: {null}, new64.f: {new66}
			a = null;
			// a: {null}, b: {new62}, c: {new64}, d: {new66}, new62.f: {null}, new64.f: {new66}
		}
		// a: {null, new62, new64}, b: {new62}, c: {new64}, d: {new66}, new62.f: {null}, new64.f: {new66}

		Test m = a.f;
		// a: {null, new62, new64}, b: {new62}, c: {new64}, d: {new66}, new62.f: {null}, new64.f: {new66}, m: {null, new66}
		Test n = new Test();
		// a: {null, new62, new64}, b: {new62}, c: {new64}, d: {new66}, new62.f: {null}, new64.f: {new66}, m: {null, new66}, n: {new91}
		a.f = n;
		// a: {null, new62, new64}, b: {new62}, c: {new64}, d: {new66}, new62.f: {null, new91}, new64.f: {new66, new91}, m: {null, new66}, n: {new91}
		m = a.f;
		// a: {null, new62, new64}, b: {new62}, c: {new64}, d: {new66}, new62.f: {null, new91}, new64.f: {new66, new91}, m: {null, new66, new91}, n: {new91}
	}

	static void public_05(int i) {
		int[] a = new int[3];
		// a: {new100}
		int[] b = new int[4];
		// a: {new100}, b: {new102}
		int[] c;
		// a: {new100}, b: {new102}, c: {null}
		if(i > 0) {
			// a: {new100}, b: {new102}, c: {null}
			c = a;
			// a: {new100}, b: {new102}, c: {new100}
		}
		else {
			// a: {new100}, b: {new102}, c: {null}
			c = b;
			// a: {new100}, b: {new102}, c: {new102}
		}
		// a: {new100}, b: {new102}, c: {new100, new102}
	}

	static void public_06() {
		Test a = new Test();
		// a: {new120}
		Test b = a;
		// a: {new120}, b: {new120}
		Test c = new Test();
		// a: {new120}, b: {new120}, c: {new124}
		Test d = null;
		// a: {new120}, b: {new120}, c: {new124}, d: {null}
		if(a == b) {
			// a: {new120}, b: {new120}, c: {new124}, d: {null}
			d = a;
			// a: {new120}, b: {new120}, c: {new124}, d: {new120}
		}
		else {
			// a: {new120}, b: {new120}, c: {new124}, d: {null}
			d = c;
			// a: {new120}, b: {new120}, c: {new124}, d: {new124}
		}
		// a: {new120}, b: {new120}, c: {new124}, d: {new120, new124}
	}

	static void public_07(int i) {
		Test a = new Test();
		// a: {new142}
		Test b = new Test();
		// a: {new142}, b: {new144}
		if(i > 0) {
			// a: {new142}, b: {new144}
			b.f = a;
			// a: {new142}, b: {new144}, new144.f: {new142}
		}
		else {
			// a: {new142}, b: {new144}
			b.f = b;
			// a: {new142}, b: {new144}, new144.f: {new144}
		}
		// a: {new142}, b: {new144}, new144.f: {new142, new144}
		if(a == b.f) {
			// a: {new142}, b: {new144}, new144.f: {new142}
			a.f = b.f;
			// a: {new142}, b: {new144}, new142.f: {new142}, new144.f: {new142}
		}
		else {
			// a: {new142}, b: {new144}, new144.f: {new144}
			a.f = b;
			// a: {new142}, b: {new144}, new142.f: {new144}, new144.f: {new144}
		}
		// a: {new142}, b: {new144}, new142.f: {new144}, new144.f: {new144}
	}

	public static void main(String[] args) {
		System.out.println("Running Test");
		public_01();
		public_02(0);
		public_03(0);
		public_04(100);
		public_05(0);
		public_06();
		public_07(0);
		System.out.println("Completed");
	}
}