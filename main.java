package com.traiana.bundle.setup.dma.ma;

import org.powermock.reflect.Whitebox;
import java.util.Arrays;
import org.mockito.ArgumentMatchers;
import java.sql.Connection;
import com.traiana.bundle.fxweb.dna.screen.DNABean;
import java.util.HashMap;
import com.traiana.bundle.fxweb.dna.screen.ExpandChanges;
import java.util.ArrayList;
import org.mockito.stubbing.Answer;
import java.math.BigDecimal;
import org.mockito.invocation.InvocationOnMock;
import java.sql.SQLException;
import org.mockito.ArgumentCaptor;
import java.util.Map;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.api.mockito.PowerMockito;
import com.traiana.bundle.fxweb.dna.screen.NarrowChanges;
import com.traiana.bundle.fxweb.dna.screen.MaLimitTenorForEdnBean;
import com.traiana.bundle.setup.dma.ma.MaSaveOp;
import com.traiana.bundle.fxweb.dna.screen.MasterAgreementBean;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.modules.junit4.PowerMockRunner;
import org.mockito.Mockito;
import java.util.List;
import java.sql.Statement;
import org.junit.Assert;
import java.util.Collections;
import com.traiana.workspace.util.NumberUtil;
import com.traiana.bundle.core.util.OrganizationUtil;
import com.traiana.workspace.bean.BeansWorkspace;
import com.traiana.bundle.fxweb.utils.HarmonyWebUtil;
import com.traiana.bundle.setup.dma.dna.DnaOp;


@RunWith(PowerMockRunner.class)
@PrepareForTest({
    MaSaveOp.class, 
    DNABean.class, 
    NumberUtil.class, 
    OrganizationUtil.class, 
    BeansWorkspace.class, 
    MaLimitTenorForEdnBean.class,
    HarmonyWebUtil.class,
    DnaOp.class
})
public class MaSaveOpTest {

   @Test
    public void test_doProductChangesInTheDna_noChanges() throws Exception {
        com.traiana.bundle.fxweb.dna.screen.DNABean dnaBean = Mockito.mock(com.traiana.bundle.fxweb.dna.screen.DNABean.class);
        com.traiana.bundle.fxweb.dna.screen.DNAProductBean dnaProductBean = Mockito.mock(com.traiana.bundle.fxweb.dna.screen.DNAProductBean.class);
        com.traiana.bundle.fxweb.dna.screen.DNABean.Tenor tenor = Mockito.mock(com.traiana.bundle.fxweb.dna.screen.DNABean.Tenor.class);
        Mockito.when(dnaBean.getDNAProductBean()).thenReturn(dnaProductBean);
        Mockito.when(dnaBean.getTenor()).thenReturn(tenor);

        com.traiana.bundle.fxweb.dna.screen.ExpandChanges expandChanges = Mockito.mock(com.traiana.bundle.fxweb.dna.screen.ExpandChanges.class);
        com.traiana.bundle.fxweb.dna.screen.NarrowChanges narrowChanges = Mockito.mock(com.traiana.bundle.fxweb.dna.screen.NarrowChanges.class);

        Mockito.when(expandChanges.getProductAdded()).thenReturn(new ArrayList<>());
        Mockito.when(narrowChanges.getProductNarrow()).thenReturn(new ArrayList<>());

        MaSaveOp target = new MaSaveOp();
        Whitebox.invokeMethod(target, "doProductChangesInTheDna", dnaBean, expandChanges, narrowChanges);

        Mockito.verifyZeroInteractions(dnaProductBean);
    }

        @Test
        public void test_doProductChangesInTheDna_addFeatures() throws Exception {
            // Arrange
            com.traiana.bundle.fxweb.dna.screen.DNABean dnaBean = org.mockito.Mockito.mock(com.traiana.bundle.fxweb.dna.screen.DNABean.class);
            com.traiana.bundle.fxweb.dna.screen.DNAProductBean dnaProductBean = org.mockito.Mockito.mock(com.traiana.bundle.fxweb.dna.screen.DNAProductBean.class);
            com.traiana.bundle.fxweb.dna.screen.DNABean.Tenor tenor = org.mockito.Mockito.mock(com.traiana.bundle.fxweb.dna.screen.DNABean.Tenor.class);
            org.mockito.Mockito.when(dnaBean.getDNAProductBean()).thenReturn(dnaProductBean);
            org.mockito.Mockito.when(dnaBean.getTenor()).thenReturn(tenor);
    
            com.traiana.bundle.fxweb.dna.screen.ExpandChanges expandChanges = org.mockito.Mockito.mock(com.traiana.bundle.fxweb.dna.screen.ExpandChanges.class);
            com.traiana.bundle.fxweb.dna.screen.NarrowChanges narrowChanges = org.mockito.Mockito.mock(com.traiana.bundle.fxweb.dna.screen.NarrowChanges.class);
            org.mockito.Mockito.when(narrowChanges.getProductNarrow()).thenReturn(new java.util.ArrayList<String>());
    
            java.util.List<com.traiana.bundle.fxweb.dna.screen.ExpandChanges.BoolTextValue> added = new java.util.ArrayList<>();
            String[] keys = new String[] {
                com.traiana.bundle.setup.dma.DnmFinals.PROD.FX_CASH,
                com.traiana.bundle.setup.dma.DnmFinals.PROD.SPOT,
                com.traiana.bundle.setup.dma.DnmFinals.PROD.VANILLA,
                com.traiana.bundle.setup.dma.DnmFinals.PROD.DIGITAL_OPTIONS,
                com.traiana.bundle.setup.dma.DnmFinals.PROD.EXOTIC_OPTIONS,
                com.traiana.bundle.setup.dma.DnmFinals.PROD.MULTILEG_OPTIONS,
                com.traiana.bundle.setup.dma.DnmFinals.PROD.SINGLE_BARRIER_OPTIONS,
                com.traiana.bundle.setup.dma.DnmFinals.PROD.DOUBLE_BARRIER_OPTIONS,
                com.traiana.bundle.setup.dma.DnmFinals.PROD.BASKET_OPTIONS,
                com.traiana.bundle.setup.dma.DnmFinals.PROD.ASIAN_OPTIONS,
                com.traiana.bundle.setup.dma.DnmFinals.PROD.BERMUDAN_OPTIONS,
                com.traiana.bundle.setup.dma.DnmFinals.PROD.EXOTIC_NDO,
                com.traiana.bundle.setup.dma.DnmFinals.PROD.NDF,
                com.traiana.bundle.setup.dma.DnmFinals.PROD.NDO_OPTIONS,
                com.traiana.bundle.setup.dma.DnmFinals.PROD.BULLION,
                com.traiana.bundle.setup.dma.DnmFinals.PROD.BULLION_SPOT_FARWARD,
                com.traiana.bundle.setup.dma.DnmFinals.PROD.BULLION_OPTION,
                com.traiana.bundle.setup.dma.DnmFinals.PROD.BULLION_OPTION_EXOTIC,
                com.traiana.bundle.setup.dma.DnmFinals.PROD.TENOR_FX_CASH,
                com.traiana.bundle.setup.dma.DnmFinals.PROD.TENOR_MAX,
                com.traiana.bundle.setup.dma.DnmFinals.PROD.TENOR_VANILLA,
                com.traiana.bundle.setup.dma.DnmFinals.PROD.TENOR_VANILLA_DELIVERABLE,
                com.traiana.bundle.setup.dma.DnmFinals.PROD.TENOR_DIGITAL_OPTIONS,
                com.traiana.bundle.setup.dma.DnmFinals.PROD.TENOR_EXOTIC_OPTIONS,
                com.traiana.bundle.setup.dma.DnmFinals.PROD.TENOR_FX_OPTIONS,
                com.traiana.bundle.setup.dma.DnmFinals.PROD.TENOR_BULLION
            };
    
            for (String key : keys) {
                com.traiana.bundle.fxweb.dna.screen.ExpandChanges.BoolTextValue mockVal = org.mockito.Mockito.mock(com.traiana.bundle.fxweb.dna.screen.ExpandChanges.BoolTextValue.class);
                org.mockito.Mockito.when(mockVal.getName()).thenReturn(key);
                org.mockito.Mockito.when(mockVal.getValue()).thenReturn(true);
                org.mockito.Mockito.when(mockVal.getTextValueAsNumber()).thenReturn("10");
                org.mockito.Mockito.when(mockVal.getValueType()).thenReturn("Type1");
                added.add(mockVal);
            }
    
            org.mockito.Mockito.when(expandChanges.getProductAdded()).thenReturn(added);
    
            // Act
            com.traiana.bundle.setup.dma.ma.MaSaveOp target = new com.traiana.bundle.setup.dma.ma.MaSaveOp();
            org.powermock.reflect.Whitebox.invokeMethod(target, "doProductChangesInTheDna", dnaBean, expandChanges, narrowChanges);
    
            // Assert
            org.mockito.Mockito.verify(dnaProductBean, org.mockito.Mockito.times(1)).setFxCash(true);
            org.mockito.Mockito.verify(dnaProductBean, org.mockito.Mockito.times(1)).setSpotFarward(true);
            org.mockito.Mockito.verify(tenor, org.mockito.Mockito.times(1)).setFxCashMaxNumber("10");
            org.mockito.Mockito.verify(tenor, org.mockito.Mockito.times(1)).setCashSpotFwdMaxNumber("10");
            org.mockito.Mockito.verify(tenor, org.mockito.Mockito.times(1)).setVanillaMaxNumber("10");
            org.mockito.Mockito.verify(tenor, org.mockito.Mockito.times(1)).setExoticOptionMaxNumber("10");
            org.mockito.Mockito.verify(tenor, org.mockito.Mockito.times(1)).setFxOptionMaxNumber("10");
        }

        @Test
        public void test_doProductChangesInTheDna_removeFeatures() throws Exception {
            // Arrange
            com.traiana.bundle.fxweb.dna.screen.DNABean dnaBean = org.mockito.Mockito.mock(com.traiana.bundle.fxweb.dna.screen.DNABean.class);
            com.traiana.bundle.fxweb.dna.screen.DNAProductBean dnaProductBean = org.mockito.Mockito.mock(com.traiana.bundle.fxweb.dna.screen.DNAProductBean.class);
            com.traiana.bundle.fxweb.dna.screen.DNABean.Tenor tenor = org.mockito.Mockito.mock(com.traiana.bundle.fxweb.dna.screen.DNABean.Tenor.class);
            org.mockito.Mockito.when(dnaBean.getDNAProductBean()).thenReturn(dnaProductBean);
            org.mockito.Mockito.when(dnaBean.getTenor()).thenReturn(tenor);
    
            org.mockito.Mockito.when(dnaProductBean.isSpotFarward()).thenReturn(true);
            org.mockito.Mockito.when(dnaProductBean.isEuropeanOptions()).thenReturn(true);
            org.mockito.Mockito.when(dnaProductBean.isBullionSpotFarward()).thenReturn(true);
            org.mockito.Mockito.when(dnaProductBean.isSingleBarrierOptions()).thenReturn(true);
            org.mockito.Mockito.when(dnaProductBean.isEuropeanDigital()).thenReturn(true);
    
            com.traiana.bundle.fxweb.dna.screen.ExpandChanges expandChanges = org.mockito.Mockito.mock(com.traiana.bundle.fxweb.dna.screen.ExpandChanges.class);
            com.traiana.bundle.fxweb.dna.screen.NarrowChanges narrowChanges = org.mockito.Mockito.mock(com.traiana.bundle.fxweb.dna.screen.NarrowChanges.class);
            org.mockito.Mockito.when(expandChanges.getProductAdded()).thenReturn(new java.util.ArrayList<com.traiana.bundle.fxweb.dna.screen.ExpandChanges.BoolTextValue>());
    
            java.util.List<String> narrowed = new java.util.ArrayList<>();
            String[] narrowKeys = new String[] {
                com.traiana.bundle.setup.dma.DnmFinals.PROD.FX_CASH,
                com.traiana.bundle.setup.dma.DnmFinals.PROD.SPOT,
                com.traiana.bundle.setup.dma.DnmFinals.PROD.FORWARD,
                com.traiana.bundle.setup.dma.DnmFinals.PROD.NDF,
                com.traiana.bundle.setup.dma.DnmFinals.PROD.FX_OPTIONS,
                com.traiana.bundle.setup.dma.DnmFinals.PROD.VANILLA,
                com.traiana.bundle.setup.dma.DnmFinals.PROD.EUROPEAN_OPTIONS,
                com.traiana.bundle.setup.dma.DnmFinals.PROD.AMERICAN_OPTIONS,
                com.traiana.bundle.setup.dma.DnmFinals.PROD.NDO_OPTIONS,
                com.traiana.bundle.setup.dma.DnmFinals.PROD.EXOTIC_OPTIONS,
                com.traiana.bundle.setup.dma.DnmFinals.PROD.MULTILEG_OPTIONS,
                com.traiana.bundle.setup.dma.DnmFinals.PROD.SINGLE_BARRIER_OPTIONS,
                com.traiana.bundle.setup.dma.DnmFinals.PROD.DOUBLE_BARRIER_OPTIONS,
                com.traiana.bundle.setup.dma.DnmFinals.PROD.DIGITAL_OPTIONS,
                com.traiana.bundle.setup.dma.DnmFinals.PROD.EUROPEAN_DIGITAL,
                com.traiana.bundle.setup.dma.DnmFinals.PROD.EUROPEAN_DIGITAL_WITH_KNOCK_IN,
                com.traiana.bundle.setup.dma.DnmFinals.PROD.EUROPEAN_DIGITAL_WITH_KNOCK_OU,
                com.traiana.bundle.setup.dma.DnmFinals.PROD.ONE_TOUCH_PAYOUT_AT_MATURITY,
                com.traiana.bundle.setup.dma.DnmFinals.PROD.ONE_TOUCH_INSTANT_PAYOUT,
                com.traiana.bundle.setup.dma.DnmFinals.PROD.NO_TOUCH,
                com.traiana.bundle.setup.dma.DnmFinals.PROD.DOUBLE_ONE_TOUCH,
                com.traiana.bundle.setup.dma.DnmFinals.PROD.DOUBLE_NO_TOUCH,
                com.traiana.bundle.setup.dma.DnmFinals.PROD.WINDOW_SINGLE_BARRIER_OPTIONS,
                com.traiana.bundle.setup.dma.DnmFinals.PROD.WINDOW_DOUBLE_BARRIER_OPTIONS,
                com.traiana.bundle.setup.dma.DnmFinals.PROD.BASKET_OPTIONS,
                com.traiana.bundle.setup.dma.DnmFinals.PROD.ASIAN_OPTIONS,
                com.traiana.bundle.setup.dma.DnmFinals.PROD.BERMUDAN_OPTIONS,
                com.traiana.bundle.setup.dma.DnmFinals.PROD.EXOTIC_NDO,
                com.traiana.bundle.setup.dma.DnmFinals.PROD.BULLION,
                com.traiana.bundle.setup.dma.DnmFinals.PROD.BULLION_SPOT_FARWARD,
                com.traiana.bundle.setup.dma.DnmFinals.PROD.BULLION_OPTION,
                com.traiana.bundle.setup.dma.DnmFinals.PROD.BULLION_OPTION_EXOTIC
            };
            for (String nk : narrowKeys) {
                narrowed.add(nk);
            }
    
            org.mockito.Mockito.when(narrowChanges.getProductNarrow()).thenReturn(narrowed);
    
            // Act
            com.traiana.bundle.setup.dma.ma.MaSaveOp target = new com.traiana.bundle.setup.dma.ma.MaSaveOp();
            org.powermock.reflect.Whitebox.invokeMethod(target, "doProductChangesInTheDna", dnaBean, expandChanges, narrowChanges);
    
            // Assert
            org.mockito.Mockito.verify(dnaProductBean, org.mockito.Mockito.times(1)).setFxCash(false);
            org.mockito.Mockito.verify(dnaProductBean, org.mockito.Mockito.times(1)).setVanillaOptions(false);
            org.mockito.Mockito.verify(dnaProductBean, org.mockito.Mockito.times(1)).setBullion(false);
            org.mockito.Mockito.verify(dnaProductBean, org.mockito.Mockito.times(1)).setEuropeanDigital(false);
            org.mockito.Mockito.verify(dnaProductBean, org.mockito.Mockito.times(1)).setSingleBarrierOptions(false);
            org.mockito.Mockito.verify(dnaProductBean, org.mockito.Mockito.times(1)).setMultiLegOptions(false);
            org.mockito.Mockito.verify(dnaProductBean, org.mockito.Mockito.times(1)).setDoubleBarrierOptions(false);
            org.mockito.Mockito.verify(dnaProductBean, org.mockito.Mockito.times(1)).setDigitalOptions(false);
        }

        @Test
        public void test_doProductChangesInTheDna_nullifyMaxNumbers() throws Exception {
            // Arrange
            com.traiana.bundle.fxweb.dna.screen.DNABean dnaBean = org.mockito.Mockito.mock(com.traiana.bundle.fxweb.dna.screen.DNABean.class);
            com.traiana.bundle.fxweb.dna.screen.DNAProductBean dnaProductBean = org.mockito.Mockito.mock(com.traiana.bundle.fxweb.dna.screen.DNAProductBean.class);
            com.traiana.bundle.fxweb.dna.screen.DNABean.Tenor tenor = org.mockito.Mockito.mock(com.traiana.bundle.fxweb.dna.screen.DNABean.Tenor.class);
            org.mockito.Mockito.when(dnaBean.getDNAProductBean()).thenReturn(dnaProductBean);
            org.mockito.Mockito.when(dnaBean.getTenor()).thenReturn(tenor);
    
            com.traiana.bundle.fxweb.dna.screen.ExpandChanges expandChanges = org.mockito.Mockito.mock(com.traiana.bundle.fxweb.dna.screen.ExpandChanges.class);
            com.traiana.bundle.fxweb.dna.screen.NarrowChanges narrowChanges = org.mockito.Mockito.mock(com.traiana.bundle.fxweb.dna.screen.NarrowChanges.class);
    
            java.util.List<com.traiana.bundle.fxweb.dna.screen.ExpandChanges.BoolTextValue> added = new java.util.ArrayList<>();
            com.traiana.bundle.fxweb.dna.screen.ExpandChanges.BoolTextValue mockVal = org.mockito.Mockito.mock(com.traiana.bundle.fxweb.dna.screen.ExpandChanges.BoolTextValue.class);
            org.mockito.Mockito.when(mockVal.getName()).thenReturn("DUMMY_PRODUCT");
            org.mockito.Mockito.when(mockVal.getValue()).thenReturn(true);
            added.add(mockVal);
    
            org.mockito.Mockito.when(expandChanges.getProductAdded()).thenReturn(added);
            org.mockito.Mockito.when(narrowChanges.getProductNarrow()).thenReturn(new java.util.ArrayList<String>());
    
            // Act
            com.traiana.bundle.setup.dma.ma.MaSaveOp target = new com.traiana.bundle.setup.dma.ma.MaSaveOp();
            org.powermock.reflect.Whitebox.invokeMethod(target, "doProductChangesInTheDna", dnaBean, expandChanges, narrowChanges);
    
            // Assert
            org.mockito.Mockito.verify(tenor, org.mockito.Mockito.times(1)).setCashSpotFwdMaxNumber(null);
            org.mockito.Mockito.verify(tenor, org.mockito.Mockito.times(1)).setFxCashMaxNumber(null);
            org.mockito.Mockito.verify(tenor, org.mockito.Mockito.times(1)).setVanillaMaxNumber(null);
            org.mockito.Mockito.verify(tenor, org.mockito.Mockito.times(1)).setVanillaDeliverableMaxNumber(null);
            org.mockito.Mockito.verify(tenor, org.mockito.Mockito.times(1)).setDigitalMaxNumber(null);
            org.mockito.Mockito.verify(tenor, org.mockito.Mockito.times(1)).setExoticOptionMaxNumber(null);
            org.mockito.Mockito.verify(tenor, org.mockito.Mockito.times(1)).setFxOptionMaxNumber(null);
            org.mockito.Mockito.verify(tenor, org.mockito.Mockito.times(1)).setBullionMaxNumber(null);
        }

        @Test
        public void test_expandValidation_normalFlow_empty() throws Exception {
            com.traiana.bundle.setup.dma.ma.MaSaveOp maSaveOp = org.powermock.reflect.Whitebox.newInstance(com.traiana.bundle.setup.dma.ma.MaSaveOp.class);
            com.traiana.bundle.fxweb.dna.screen.MasterAgreementBean maBean = Mockito.mock(com.traiana.bundle.fxweb.dna.screen.MasterAgreementBean.class);
            com.traiana.bundle.fxweb.dna.screen.ExpandChanges expandChanges = Mockito.mock(com.traiana.bundle.fxweb.dna.screen.ExpandChanges.class);
            org.powermock.reflect.Whitebox.setInternalState(maSaveOp, "maBean", maBean);
            Mockito.when(maBean.getExpandChanges()).thenReturn(expandChanges);
            Mockito.when(expandChanges.getProductAdded()).thenReturn(new java.util.ArrayList<com.traiana.bundle.fxweb.dna.screen.ExpandChanges.BoolTextValue>());
            Mockito.when(expandChanges.getTenorsAdded()).thenReturn(new java.util.ArrayList<com.traiana.bundle.fxweb.dna.screen.ExpandChanges.BoolTextValue>());
            Mockito.when(expandChanges.getLimitsAdded()).thenReturn(new java.util.ArrayList<com.traiana.bundle.fxweb.dna.screen.ExpandChanges.BoolTextValue>());
            Mockito.when(expandChanges.getCurrenciesAdded_FXCash()).thenReturn(new java.util.ArrayList<com.traiana.bundle.fxweb.dna.screen.ExpandChanges.BoolValue>());
            Mockito.when(expandChanges.getCurrenciesAdded_NDF()).thenReturn(new java.util.ArrayList<com.traiana.bundle.fxweb.dna.screen.ExpandChanges.BoolValue>());
            Mockito.when(expandChanges.getCurrenciesAdded_FXOptions()).thenReturn(new java.util.ArrayList<com.traiana.bundle.fxweb.dna.screen.ExpandChanges.BoolValue>());
            Mockito.when(expandChanges.getCurrenciesAdded_NDO()).thenReturn(new java.util.ArrayList<com.traiana.bundle.fxweb.dna.screen.ExpandChanges.BoolValue>());
            Mockito.when(expandChanges.getCurrenciesAdded_ExcticOptions()).thenReturn(new java.util.ArrayList<com.traiana.bundle.fxweb.dna.screen.ExpandChanges.BoolValue>());
            Mockito.when(expandChanges.getCurrenciesAdded_ExcticNDO()).thenReturn(new java.util.ArrayList<com.traiana.bundle.fxweb.dna.screen.ExpandChanges.BoolValue>());
            Mockito.when(expandChanges.getCurrenciesAdded_Bullion()).thenReturn(new java.util.ArrayList<com.traiana.bundle.fxweb.dna.screen.ExpandChanges.BoolValue>());
            Mockito.when(expandChanges.getCurrenciesAdded_Bullion_ccyPairs()).thenReturn(new java.util.ArrayList<com.traiana.bundle.fxweb.dna.screen.ExpandChanges.BoolValue>());
            Mockito.when(expandChanges.getMastSelectedCurrencyTypeMap()).thenReturn(new java.util.HashMap<java.lang.Object, java.lang.Object>());
            org.powermock.reflect.Whitebox.invokeMethod(maSaveOp, "expandValidation");
            Mockito.verify(expandChanges, Mockito.times(1)).getProductAdded();
            Mockito.verify(expandChanges, Mockito.times(1)).getTenorsAdded();
        }

        @Test
        public void test_expandValidation_productsAndTenorsAdded() throws Exception {
            com.traiana.bundle.setup.dma.ma.MaSaveOp maSaveOp = org.powermock.reflect.Whitebox.newInstance(com.traiana.bundle.setup.dma.ma.MaSaveOp.class);
          