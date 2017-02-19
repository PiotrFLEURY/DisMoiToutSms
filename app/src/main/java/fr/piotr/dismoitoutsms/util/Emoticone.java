package fr.piotr.dismoitoutsms.util;

import fr.piotr.dismoitoutsms.R;

public enum Emoticone {

	//@formatter:off
	ANGE("O:-)", R.string.emoticone_ange),
	SOURRIRE(":-)", R.string.emoticone_sourrire),
	TRISTE(":-(", R.string.emoticone_triste),
	PLEUR(":'(", R.string.emoticone_pleur),
	CLEIN_D_OEIL(";-)", R.string.emoticone_clein_d_oeil),
	TIRER_LA_LANGUE(":-P", R.string.emoticone_tirer_la_langue),
	SURPRIS("=-O", R.string.emoticone_surpris),
	BISOU(":-*", R.string.emoticone_bisou),
	HURLEMENT(":O", R.string.emoticone_hurlement),
	COOL("B-)", R.string.emoticone_cool),
	DECU(":-!", R.string.emoticone_decu),
	BOUCHE_COUSUE(":-X", R.string.emoticone_bouche_cousue),
	RIRES(":-D", R.string.emoticone_rires),
	CONFUS("o_O", R.string.emoticone_confus),
	EMBARRASSE(":-[", R.string.emoticone_embarrasse),
	INDECIS(":-\\", R.string.emoticone_indecis),
	PERSPECTIVE_D_ARGENT(":-$", R.string.emoticone_perspective_d_argent),
	CIRCONFLEX("^^", R.string.emoticone_circonflex);
	//@formatter:off

	private String	code;
	private int	remplacement;

	Emoticone(String code, int remplacement) {
		this.code = code;
		this.remplacement = remplacement;
	}

	/**
	 * @return the code
	 */
	public String getCode() {
		return code;
	}

	/**
	 * @return the remplacement
	 */
	public int getRemplacement() {
		return remplacement;
	}

	public static String getCodesForTest(){
		StringBuilder sb=new StringBuilder();
		
		for(final Emoticone emo:values()){
			if(!sb.toString().isEmpty()){
				sb.append(", ");
			}
			sb.append(emo.getCode());
		}
		
		return sb.toString();
	}

}
