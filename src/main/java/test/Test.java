package test;

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
			// a: {new37}, b: {new39, new41, new47}, c: {new41}, new39.f: {new41, new47}, new41.f: {new47}, new47.f: {new47}
			b.f = new Test();
			// a: {new37}, b: {new39, new41, new47}, c: {new41}, new39.f: {new41, new47}, new41.f: {new47}, new47.f: {new47}
			b = b.f;
			// a: {new37}, b: {new41, new47}, c: {new41}, new39.f: {new41, new47}, new41.f: {new47}, new47.f: {new47}
			value += 1;
			// a: {new37}, b: {new41, new47}, c: {new41}, new39.f: {new41, new47}, new41.f: {new47}, new47.f: {new47}
		}
		// a: {new37}, b: {new39, new41, new47}, c: {new41}, new39.f: {new41, new47}, new41.f: {new47}, new47.f: {new47}
		c.f = b.f;
		// a: {new37}, b: {new39, new47}, c: {new41}, new39.f: {new41, new47}, new41.f: {new41, new47}
	}

	@SuppressWarnings("null")
	static void public_04(int value) {
		Test a = null;
		// a: {null}
		Test b = new Test();
		// a: {null}, b: {new63}
		Test c = new Test();
		// a: {null}, b: {new63}, c: {new65}
		Test d = new Test();
		// a: {null}, b: {new63}, c: {new65}, d: {new67}
		b.f = null;
		// a: {null}, b: {new63}, c: {new65}, d: {new67}, new63.f: {null}
		c.f = d;
		// a: {null}, b: {new63}, c: {new65}, d: {new67}, new63.f: {null}, new65.f: {new67}
		if(value == 100) {
			// a: {null}, b: {new63}, c: {new65}, d: {new67}, new63.f: {null}, new65.f: {new67}
			a = b;
			// a: {new63}, b: {new63}, c: {new65}, d: {new67}, new63.f: {null}, new65.f: {new67}
		}
		else if (value == 200) {
			// a: {null}, b: {new63}, c: {new65}, d: {new67}, new63.f: {null}, new65.f: {new67}
			a = c;
			// a: {new65}, b: {new63}, c: {new65}, d: {new67}, new63.f: {null}, new65.f: {new67}
		}
		else {
			// a: {null}, b: {new63}, c: {new65}, d: {new67}, new63.f: {null}, new65.f: {new67}
			a = null;
			// a: {null}, b: {new63}, c: {new65}, d: {new67}, new63.f: {null}, new65.f: {new67}
		}
		// a: {null, new63, new65}, b: {new63}, c: {new65}, d: {new67}, new63.f: {null}, new65.f: {new67}

		Test m = a.f;
		// a: {null, new63, new65}, b: {new63}, c: {new65}, d: {new67}, new63.f: {null}, new65.f: {new67}, m: {null, new67}
		Test n = new Test();
		// a: {null, new63, new65}, b: {new63}, c: {new65}, d: {new67}, new63.f: {null}, new65.f: {new67}, m: {null, new67}, n: {new92}
		a.f = n;
		// a: {null, new63, new65}, b: {new63}, c: {new65}, d: {new67}, new63.f: {null, new92}, new65.f: {new67, new92}, m: {null, new67}, n: {new92}
		m = a.f;
		// a: {null, new63, new65}, b: {new63}, c: {new65}, d: {new67}, new63.f: {null, new92}, new65.f: {new67, new92}, m: {null, new67, new92}, n: {new92}
	}

	static void public_05(int i) {
		int[] a = new int[3];
		// a: {new101}
		int[] b = new int[4];
		// a: {new101}, b: {new103}
		int[] c;
		// a: {new101}, b: {new103}
		if(i > 0) {
			// a: {new101}, b: {new103}
			c = a;
			// a: {new101}, b: {new103}, c: {new101}
		}
		else {
			// a: {new101}, b: {new103}
			c = b;
			// a: {new101}, b: {new103}, c: {new103}
		}
		// a: {new101}, b: {new103}, c: {new101, new103}
		c[0] = 1;
		// a: {new101}, b: {new103}, c: {new101, new103}
	}

	static void public_06() {
		Test a = new Test();
		// a: {new123}
		Test b = a;
		// a: {new123}, b: {new123}
		Test c = new Test();
		// a: {new123}, b: {new123}, c: {new127}
		Test d;
		// a: {new123}, b: {new123}, c: {new127}
		if(a == b) {
			// a: {new123}, b: {new123}, c: {new127}
			d = a;
			// a: {new123}, b: {new123}, c: {new127}, d: {new123}
		}
		else {
			// a: {new123}, b: {new123}, c: {new127}
			d = c;
			// a: {new123}, b: {new123}, c: {new127}, d: {new127}
		}
		// a: {new123}, b: {new123}, c: {new127}, d: {new123, new127}
		d.f = b;
		// a: {new123}, b: {new123}, c: {new127}, d: {new123, new127}, new123.f: {new123}, new127.f: {new123}
	}

	static void public_07(int i) {
		Test a = new Test();
		// a: {new147}
		Test b = new Test();
		// a: {new147}, b: {new149}
		Test c;
		// a: {new147}, b: {new149}
		if(i > 0) {
			// a: {new147}, b: {new149}
			b.f = a;
			// a: {new147}, b: {new149}, new149.f: {new147}
		}
		else {
			// a: {new147}, b: {new149}
			b.f = b;
			// a: {new147}, b: {new149}, new149.f: {new149}
		}
		// a: {new147}, b: {new149}, new149.f: {new147, new149}
		if(a == b.f) {
			// a: {new147}, b: {new149}, new149.f: {new147, new149}
			a.f = b.f;
			// a: {new147}, b: {new149}, new147.f: {new147, new149}, new149.f: {new147}
		}
		else {
			// a: {new147}, b: {new149}, new149.f: {new147, new149}
			a.f = b;
			// a: {new147}, b: {new149}, new147.f: {new149}, new149.f: {new149}
		}
		// a: {new147}, b: {new149}, new147.f: {new147, new149}, new149.f: {new147, new149}
		c = a.f;
		// a: {new147}, b: {new149}, new147.f: {new147, new149}, new149.f: {new147, new149}, c: {new147, new149}
	}

	public static void main(String[] args) {
		/* In case you need to run the main function in the Test class, use the command
		 *		mvn clean package exec:java@test -q
		 */
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