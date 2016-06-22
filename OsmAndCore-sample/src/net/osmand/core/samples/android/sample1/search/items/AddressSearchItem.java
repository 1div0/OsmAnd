package net.osmand.core.samples.android.sample1.search.items;

import net.osmand.core.jni.Address;
import net.osmand.core.jni.ObfAddressStreetGroupSubtype;
import net.osmand.core.jni.ObfAddressStreetGroupType;
import net.osmand.core.jni.Street;
import net.osmand.core.jni.StreetGroup;
import net.osmand.util.Algorithms;

import java.math.BigInteger;

public class AddressSearchItem extends SearchItem {

	private String namePrefix;
	private String nameSuffix;
	private String typeStr;

	private BigInteger parentCityObfId;
	private BigInteger parentPostcodeObfId;

	public AddressSearchItem(Address address) {
		super();

		switch (address.getAddressType()) {
			case Street:
				StreetInternal street = new StreetInternal(address);
				setLocation(street.getPosition31());
				setNativeName(street.getNativeName());
				addLocalizedNames(street.getLocalizedNames());
				if (street.getStreetGroup() != null) {
					StreetGroup streetGroup = street.getStreetGroup();
					nameSuffix = "st.";
					typeStr = streetGroup.getNativeName() + " — " + getTypeStr(streetGroup);
					if (streetGroup.getType() == ObfAddressStreetGroupType.Postcode) {
						parentPostcodeObfId = streetGroup.getId().getId();
					} else {
						parentCityObfId = streetGroup.getId().getId();
					}
				} else {
					typeStr = "Street";
				}
				break;

			case StreetGroup:
				StreetGroupInternal streetGroup = new StreetGroupInternal(address);
				setLocation(streetGroup.getPosition31());
				setNativeName(streetGroup.getNativeName());
				addLocalizedNames(streetGroup.getLocalizedNames());
				typeStr = getTypeStr(streetGroup);
				break;
		}
	}

	public String getNamePrefix() {
		return namePrefix;
	}

	public String getNameSuffix() {
		return nameSuffix;
	}

	@Override
	public String getName() {
		StringBuilder sb = new StringBuilder();
		if (!Algorithms.isEmpty(namePrefix)) {
			sb.append(namePrefix);
			sb.append(" ");
		}
		sb.append(super.getName());
		if (!Algorithms.isEmpty(nameSuffix)) {
			sb.append(" ");
			sb.append(nameSuffix);
		}
		return sb.toString();
	}

	@Override
	public String getType() {
		return typeStr;
	}

	private String getTypeStr(StreetGroup streetGroup) {
		String typeStr;
		if (streetGroup.getSubtype() != ObfAddressStreetGroupSubtype.Unknown) {
			typeStr = streetGroup.getSubtype().name();
		} else {
			typeStr = streetGroup.getType().name();
		}
		return typeStr;
	}

	public BigInteger getParentCityObfId() {
		return parentCityObfId;
	}

	public BigInteger getParentPostcodeObfId() {
		return parentPostcodeObfId;
	}

	private class StreetInternal extends Street {
		public StreetInternal(Address address) {
			super(Address.getCPtr(address), false);
		}
	}

	private class StreetGroupInternal extends StreetGroup {
		public StreetGroupInternal(Address address) {
			super(Address.getCPtr(address), false);
		}
	}
}
