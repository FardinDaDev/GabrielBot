package gabrielbot.lang.common;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ConstantPool {
	private final List<String> list;
	
	public ConstantPool(List<String> list) {
		this.list = Collections.unmodifiableList(list);
	}
	
	public String get(int i) {
		return list.get(i);
	}
	
	public List<String> asList() {
		return list;
	}
	
	public void writeTo(DataOutput dos) {
		try {
			dos.writeInt(Constants.CONSTANT_POOL_START);
			dos.writeInt(list.size());
			for(String s : list) {
				dos.writeUTF(s);
			}
		} catch(IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	public static ConstantPool readFrom(DataInput dis) {
		try {
			int count = dis.readInt();
			List<String> l = new ArrayList<>();
			for(int i = 0; i < count; i++) {
				l.add(dis.readUTF());
			}
			return new ConstantPool(l);
		} catch(IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	public static class Builder {
		private int labels = 0;
		private List<String> constants = new ArrayList<>();
		
		public int add(String o) {
			if(o.isEmpty()) throw new Error(o);
			if(!constants.contains(o))
				constants.add(o);
			return constants.indexOf(o);
		}
		
		public int nextLabel() {
			labels++;
			return add("label_" + labels);
		}
		
		public ConstantPool build() {
			return new ConstantPool(constants);
		}
	}
}
