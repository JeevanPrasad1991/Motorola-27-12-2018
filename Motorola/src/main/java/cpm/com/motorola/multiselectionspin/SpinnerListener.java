package cpm.com.motorola.multiselectionspin;

import java.util.ArrayList;

import cpm.com.motorola.xmlgettersetter.SaleTeamGetterSetter;


/**
 * Created by jeevanp on 2/2/2018.
 */

public interface SpinnerListener {
    void onItemsSelected(ArrayList<SaleTeamGetterSetter> items);
}