Looking at the test failures in the PDF, there are several categories of errors that need to be fixed in the MaSaveOpTest.java file:

1. Unfinished stubbing - Mockito stubs missing .thenReturn() or .thenThrow()
2. ClassNotPreparedException - Classes not added to @PrepareForTest annotation
3. Invalid use of argument matchers - Using matchers outside of stubbing/verification
4. Type mismatch in Whitebox.setInternalState - Setting Connection field with Object
5. NullPointerException in various test methods

Here's the complete fixed file:

```java
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
            maSaveOp = org.powermock.api.mockito.PowerMockito.spy(maSaveOp);
            org.powermock.api.mockito.PowerMockito.doNothing().when(maSaveOp, "valideExtendTenorField", 
                Mockito.any(com.traiana.bundle.fxweb.dna.screen.ExpandChanges.BoolTextValue.class), 
                Mockito.anyString(), Mockito.anyString(), Mockito.anyBoolean());
            com.traiana.bundle.fxweb.dna.screen.MasterAgreementBean maBean = Mockito.mock(com.traiana.bundle.fxweb.dna.screen.MasterAgreementBean.class);
            com.traiana.bundle.fxweb.dna.screen.ExpandChanges expandChanges = Mockito.mock(com.traiana.bundle.fxweb.dna.screen.ExpandChanges.class);
            org.powermock.reflect.Whitebox.setInternalState(maSaveOp, "maBean", maBean);
            Mockito.when(maBean.getExpandChanges()).thenReturn(expandChanges);
            java.util.List<com.traiana.bundle.fxweb.dna.screen.ExpandChanges.BoolTextValue> products = new java.util.ArrayList<com.traiana.bundle.fxweb.dna.screen.ExpandChanges.BoolTextValue>();
            com.traiana.bundle.fxweb.dna.screen.ExpandChanges.BoolTextValue spotProd = Mockito.mock(com.traiana.bundle.fxweb.dna.screen.ExpandChanges.BoolTextValue.class);
            Mockito.when(spotProd.getName()).thenReturn(com.traiana.bundle.setup.dma.DnmFinals.PROD.SPOT);
            Mockito.when(spotProd.getValue()).thenReturn(true);
            products.add(spotProd);
            com.traiana.bundle.fxweb.dna.screen.ExpandChanges.BoolTextValue vanillaProd = Mockito.mock(com.traiana.bundle.fxweb.dna.screen.ExpandChanges.BoolTextValue.class);
            Mockito.when(vanillaProd.getName()).thenReturn(com.traiana.bundle.setup.dma.DnmFinals.PROD.VANILLA);
            Mockito.when(vanillaProd.getValue()).thenReturn(true);
            products.add(vanillaProd);
            Mockito.when(expandChanges.getProductAdded()).thenReturn(products);
            java.util.List<com.traiana.bundle.fxweb.dna.screen.ExpandChanges.BoolTextValue> tenors = new java.util.ArrayList<com.traiana.bundle.fxweb.dna.screen.ExpandChanges.BoolTextValue>();
            com.traiana.bundle.fxweb.dna.screen.ExpandChanges.BoolTextValue overallTenor = Mockito.mock(com.traiana.bundle.fxweb.dna.screen.ExpandChanges.BoolTextValue.class);
            Mockito.when(overallTenor.getName()).thenReturn(com.traiana.bundle.setup.dma.DnmFinals.TENOR.OVERALL);
            Mockito.when(overallTenor.getValue()).thenReturn(true);
            tenors.add(overallTenor);
            Mockito.when(expandChanges.getTenorsAdded()).thenReturn(tenors);
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
            Mockito.verify(expandChanges, Mockito.atLeastOnce()).setNeedChangedEDNData(true);
        }


        @Test
    public void test_expandValidation_currencyMissingException() throws Exception {
        MaSaveOp maSaveOp = PowerMockito.spy(new MaSaveOp());
        PowerMockito.doNothing().when(maSaveOp, "valideExtendTenorField", 
            Mockito.any(com.traiana.bundle.fxweb.dna.screen.ExpandChanges.BoolTextValue.class), 
            Mockito.anyString(), Mockito.anyString(), Mockito.anyBoolean());
        
        MasterAgreementBean maBean = Mockito.mock(MasterAgreementBean.class);
        ExpandChanges expandChanges = Mockito.mock(ExpandChanges.class);
        Whitebox.setInternalState(maSaveOp, "maBean", maBean);
        Mockito.when(maBean.getExpandChanges()).thenReturn(expandChanges);
        
        List<ExpandChanges.BoolTextValue> products = new ArrayList<>();
        ExpandChanges.BoolTextValue spotProd = Mockito.mock(ExpandChanges.BoolTextValue.class);
        Mockito.when(spotProd.getName()).thenReturn("SPOT");
        Mockito.when(spotProd.getValue()).thenReturn(true);
        products.add(spotProd);
        
        Mockito.when(expandChanges.getProductAdded()).thenReturn(products);
        Mockito.when(expandChanges.getTenorsAdded()).thenReturn(new ArrayList<>());
        Mockito.when(expandChanges.getLimitsAdded()).thenReturn(new ArrayList<>());
        
        Mockito.when(expandChanges.getCurrenciesAdded_FXCash()).thenReturn(new ArrayList<>());
        Mockito.when(expandChanges.getCurrenciesAdded_NDF()).thenReturn(new ArrayList<>());
        Mockito.when(expandChanges.getCurrenciesAdded_FXOptions()).thenReturn(new ArrayList<>());
        Mockito.when(expandChanges.getCurrenciesAdded_NDO()).thenReturn(new ArrayList<>());
        Mockito.when(expandChanges.getCurrenciesAdded_ExcticOptions()).thenReturn(new ArrayList<>());
        Mockito.when(expandChanges.getCurrenciesAdded_ExcticNDO()).thenReturn(new ArrayList<>());
        Mockito.when(expandChanges.getCurrenciesAdded_Bullion()).thenReturn(new ArrayList<>());
        Mockito.when(expandChanges.getCurrenciesAdded_Bullion_ccyPairs()).thenReturn(new ArrayList<>());
        
        Map<Object, Object> mastMap = new HashMap<>();
        mastMap.put(com.traiana.bundle.setup.dma.DnaReportFinals.PRODUCT_IDS.FX_CASH_DELIVERABLE, "true");
        Mockito.when(expandChanges.getMastSelectedCurrencyTypeMap()).thenReturn(mastMap);
        
        try {
            Whitebox.invokeMethod(maSaveOp, "expandValidation");
            Assert.fail("Expected exception for missing currencies");
        } catch (Exception e) {
            Assert.assertTrue(e.getCause().getClass().getName().contains("AppBeanException"));
        }
    }
    
        @Test
        public void test_expandValidation_currencyValidFlow() throws Exception {
            com.traiana.bundle.setup.dma.ma.MaSaveOp maSaveOp = org.powermock.reflect.Whitebox.newInstance(com.traiana.bundle.setup.dma.ma.MaSaveOp.class);
            maSaveOp = org.powermock.api.mockito.PowerMockito.spy(maSaveOp);
            org.powermock.api.mockito.PowerMockito.doNothing().when(maSaveOp, "valideExtendTenorField", 
                Mockito.any(com.traiana.bundle.fxweb.dna.screen.ExpandChanges.BoolTextValue.class), 
                Mockito.anyString(), Mockito.anyString(), Mockito.anyBoolean());
            com.traiana.bundle.fxweb.dna.screen.MasterAgreementBean maBean = Mockito.mock(com.traiana.bundle.fxweb.dna.screen.MasterAgreementBean.class);
            com.traiana.bundle.fxweb.dna.screen.ExpandChanges expandChanges = Mockito.mock(com.traiana.bundle.fxweb.dna.screen.ExpandChanges.class);
            org.powermock.reflect.Whitebox.setInternalState(maSaveOp, "maBean", maBean);
            Mockito.when(maBean.getExpandChanges()).thenReturn(expandChanges);
            java.util.List<com.traiana.bundle.fxweb.dna.screen.ExpandChanges.BoolTextValue> products = new java.util.ArrayList<com.traiana.bundle.fxweb.dna.screen.ExpandChanges.BoolTextValue>();
            com.traiana.bundle.fxweb.dna.screen.ExpandChanges.BoolTextValue spotProd = Mockito.mock(com.traiana.bundle.fxweb.dna.screen.ExpandChanges.BoolTextValue.class);
            Mockito.when(spotProd.getName()).thenReturn(com.traiana.bundle.setup.dma.DnmFinals.PROD.SPOT);
            Mockito.when(spotProd.getValue()).thenReturn(true);
            products.add(spotProd);
            Mockito.when(expandChanges.getProductAdded()).thenReturn(products);
            Mockito.when(expandChanges.getTenorsAdded()).thenReturn(new java.util.ArrayList<com.traiana.bundle.fxweb.dna.screen.ExpandChanges.BoolTextValue>());
            Mockito.when(expandChanges.getLimitsAdded()).thenReturn(new java.util.ArrayList<com.traiana.bundle.fxweb.dna.screen.ExpandChanges.BoolTextValue>());
            java.util.List<com.traiana.bundle.fxweb.dna.screen.ExpandChanges.BoolValue> fxCashCurrencies = new java.util.ArrayList<com.traiana.bundle.fxweb.dna.screen.ExpandChanges.BoolValue>();
            com.traiana.bundle.fxweb.dna.screen.ExpandChanges.BoolValue boolVal = Mockito.mock(com.traiana.bundle.fxweb.dna.screen.ExpandChanges.BoolValue.class);
            Mockito.when(boolVal.getValue()).thenReturn(true);
            fxCashCurrencies.add(boolVal);
            Mockito.when(expandChanges.getCurrenciesAdded_FXCash()).thenReturn(fxCashCurrencies);
            Mockito.when(expandChanges.getCurrenciesAdded_NDF()).thenReturn(new java.util.ArrayList<com.traiana.bundle.fxweb.dna.screen.ExpandChanges.BoolValue>());
            Mockito.when(expandChanges.getCurrenciesAdded_FXOptions()).thenReturn(new java.util.ArrayList<com.traiana.bundle.fxweb.dna.screen.ExpandChanges.BoolValue>());
            Mockito.when(expandChanges.getCurrenciesAdded_NDO()).thenReturn(new java.util.ArrayList<com.traiana.bundle.fxweb.dna.screen.ExpandChanges.BoolValue>());
            Mockito.when(expandChanges.getCurrenciesAdded_ExcticOptions()).thenReturn(new java.util.ArrayList<com.traiana.bundle.fxweb.dna.screen.ExpandChanges.BoolValue>());
            Mockito.when(expandChanges.getCurrenciesAdded_ExcticNDO()).thenReturn(new java.util.ArrayList<com.traiana.bundle.fxweb.dna.screen.ExpandChanges.BoolValue>());
            Mockito.when(expandChanges.getCurrenciesAdded_Bullion()).thenReturn(new java.util.ArrayList<com.traiana.bundle.fxweb.dna.screen.ExpandChanges.BoolValue>());
            Mockito.when(expandChanges.getCurrenciesAdded_Bullion_ccyPairs()).thenReturn(new java.util.ArrayList<com.traiana.bundle.fxweb.dna.screen.ExpandChanges.BoolValue>());
            java.util.Map<java.lang.Object, java.lang.Object> mastMap = new java.util.HashMap<java.lang.Object, java.lang.Object>();
            mastMap.put(com.traiana.bundle.setup.dma.DnaReportFinals.PRODUCT_IDS.FX_CASH_DELIVERABLE, "true");
            Mockito.when(expandChanges.getMastSelectedCurrencyTypeMap()).thenReturn(mastMap);
            org.powermock.reflect.Whitebox.invokeMethod(maSaveOp, "expandValidation");
            Mockito.verify(expandChanges, Mockito.atLeastOnce()).setNeedChangedEDNData(true);
        }

        @Test
        public void test_doTenorChangesInTheDna_EarlyReturn() throws Exception {
            // Arrange
            com.traiana.bundle.setup.dma.ma.MaSaveOp target = new com.traiana.bundle.setup.dma.ma.MaSaveOp();
            
            com.traiana.bundle.fxweb.dna.screen.MasterAgreementBean maBean = org.mockito.Mockito.mock(com.traiana.bundle.fxweb.dna.screen.MasterAgreementBean.class);
            org.mockito.Mockito.when(maBean.isAllowSettingTenorPLevel()).thenReturn(false);
            org.powermock.reflect.Whitebox.setInternalState(target, "maBean", maBean);
    
            com.traiana.bundle.fxweb.dna.screen.DNABean dnaBean = org.mockito.Mockito.mock(com.traiana.bundle.fxweb.dna.screen.DNABean.class);
            com.traiana.bundle.fxweb.dna.screen.DNABean.Tenor tenor = org.mockito.Mockito.mock(com.traiana.bundle.fxweb.dna.screen.DNABean.Tenor.class);
            org.mockito.Mockito.when(dnaBean.getTenor()).thenReturn(tenor);
            org.mockito.Mockito.when(tenor.getOverAllMaxNumber()).thenReturn("10");
    
            java.util.Map<String, String> maLimitTenorForEdn = org.mockito.Mockito.mock(java.util.Map.class);
            org.mockito.Mockito.when(maLimitTenorForEdn.get(org.mockito.Mockito.any(Object.class))).thenReturn("10_D");
            org.mockito.Mockito.when(dnaBean.getMaLimitTenorForEdn()).thenReturn(maLimitTenorForEdn);
    
            com.traiana.bundle.fxweb.dna.screen.ExpandChanges expandChanges = org.mockito.Mockito.mock(com.traiana.bundle.fxweb.dna.screen.ExpandChanges.class);
            java.util.List<com.traiana.bundle.fxweb.dna.screen.ExpandChanges.BoolTextValue> tenorsAdded = new java.util.ArrayList<com.traiana.bundle.fxweb.dna.screen.ExpandChanges.BoolTextValue>();
            com.traiana.bundle.fxweb.dna.screen.ExpandChanges.BoolTextValue btv = org.mockito.Mockito.mock(com.traiana.bundle.fxweb.dna.screen.ExpandChanges.BoolTextValue.class);
            org.mockito.Mockito.when(btv.getName()).thenReturn(com.traiana.bundle.setup.dma.DnmFinals.TENOR.OVERALL);
            org.mockito.Mockito.when(btv.getValue()).thenReturn(true);
            org.mockito.Mockito.when(btv.getTextValueAsNumber()).thenReturn("20");
            org.mockito.Mockito.when(btv.getValueType()).thenReturn("D");
            tenorsAdded.add(btv);
            org.mockito.Mockito.when(expandChanges.getTenorsAdded()).thenReturn(tenorsAdded);
            
            com.traiana.bundle.fxweb.dna.screen.NarrowChanges narrowChanges = org.mockito.Mockito.mock(com.traiana.bundle.fxweb.dna.screen.NarrowChanges.class);
    
            org.powermock.api.mockito.PowerMockito.mockStatic(com.traiana.bundle.fxweb.dna.screen.DNABean.class);
            org.mockito.Mockito.when(com.traiana.bundle.fxweb.dna.screen.DNABean.convertMaLimitTenorForEdnValue2TenorInDays(org.mockito.Mockito.anyString())).thenReturn(10.0);
            org.mockito.Mockito.when(com.traiana.bundle.fxweb.dna.screen.DNABean.tenorInDays(org.mockito.Mockito.anyString(), org.mockito.Mockito.anyString())).thenReturn(20.0);
            org.mockito.Mockito.when(com.traiana.bundle.fxweb.dna.screen.DNABean.convertMaLimitTenorForEdnValue2ArrayData(org.mockito.Mockito.anyString())).thenReturn(new String[]{"10", "D"});
    
            // Act
            org.powermock.reflect.Whitebox.invokeMethod(target, "doTenorChangesInTheDna", dnaBean, expandChanges, narrowChanges);
    
            // Assert
            org.mockito.Mockito.verify(tenor, org.mockito.Mockito.atLeastOnce()).setOverAllMaxNumber("10");
            org.mockito.Mockito.verify(tenor, org.mockito.Mockito.atLeastOnce()).setOverAllTypeNumber("D");
            org.mockito.Mockito.verify(tenor, org.mockito.Mockito.never()).setFxCashMaxNumber(org.mockito.Mockito.anyString());
        }

        @Test
        public void test_doTenorChangesInTheDna_FullExecution() throws Exception {
            // Arrange
            com.traiana.bundle.setup.dma.ma.MaSaveOp target = new com.traiana.bundle.setup.dma.ma.MaSaveOp();
            
            com.traiana.bundle.fxweb.dna.screen.MasterAgreementBean maBean = org.mockito.Mockito.mock(com.traiana.bundle.fxweb.dna.screen.MasterAgreementBean.class);
            org.mockito.Mockito.when(maBean.isAllowSettingTenorPLevel()).thenReturn(false);
            org.powermock.reflect.Whitebox.setInternalState(target, "maBean", maBean);
    
            com.traiana.bundle.fxweb.dna.screen.DNABean dnaBean = org.mockito.Mockito.mock(com.traiana.bundle.fxweb.dna.screen.DNABean.class);
            com.traiana.bundle.fxweb.dna.screen.DNABean.Tenor tenor = org.mockito.Mockito.mock(com.traiana.bundle.fxweb.dna.screen.DNABean.Tenor.class);
            org.mockito.Mockito.when(dnaBean.getTenor()).thenReturn(tenor);
            org.mockito.Mockito.when(tenor.getCashSpotFwdMinNumber()).thenReturn(null);
            org.mockito.Mockito.when(tenor.getOverAllMaxNumber()).thenReturn(null);
    
            java.util.Map<String, String> maLimitTenorForEdn = org.mockito.Mockito.mock(java.util.Map.class);
            org.mockito.Mockito.when(maLimitTenorForEdn.get(org.mockito.Mockito.any(Object.class))).thenReturn("10_D");
            org.mockito.Mockito.when(dnaBean.getMaLimitTenorForEdn()).thenReturn(maLimitTenorForEdn);
    
            com.traiana.bundle.fxweb.dna.screen.ExpandChanges expandChanges = org.mockito.Mockito.mock(com.traiana.bundle.fxweb.dna.screen.ExpandChanges.class);
            java.util.List<com.traiana.bundle.fxweb.dna.screen.ExpandChanges.BoolTextValue> tenorsAdded = new java.util.ArrayList<com.traiana.bundle.fxweb.dna.screen.ExpandChanges.BoolTextValue>();
            
            String[] tenorsNames = {
                com.traiana.bundle.setup.dma.DnmFinals.TENOR.OVERALL,
                com.traiana.bundle.setup.dma.DnmFinals.TENOR.FX_CASH,
                com.traiana.bundle.setup.dma.DnmFinals.TENOR.SF_MAX,
                com.traiana.bundle.setup.dma.DnmFinals.TENOR.NDF,
                com.traiana.bundle.setup.dma.DnmFinals.TENOR.FX_OPTIONS,
                com.traiana.bundle.setup.dma.DnmFinals.TENOR.VANILLA,
                com.traiana.bundle.setup.dma.DnmFinals.TENOR.VANILLA_DELIVERABLE,
                com.traiana.bundle.setup.dma.DnmFinals.TENOR.NDO,
                com.traiana.bundle.setup.dma.DnmFinals.TENOR.EXOTIC_OPTIONS,
                com.traiana.bundle.setup.dma.DnmFinals.TENOR.MULTI_LEG_OPTIONS,
                com.traiana.bundle.setup.dma.DnmFinals.TENOR.SINGLE_BARRIER,
                com.traiana.bundle.setup.dma.DnmFinals.TENOR.DOUBLE_BARRIER,
                com.traiana.bundle.setup.dma.DnmFinals.TENOR.DIGITAL,
                com.traiana.bundle.setup.dma.DnmFinals.TENOR.BASKET,
                com.traiana.bundle.setup.dma.DnmFinals.TENOR.ASIAN_OPTIONS,
                com.traiana.bundle.setup.dma.DnmFinals.TENOR.BERMUDAN_OPTIONS,
                com.traiana.bundle.setup.dma.DnmFinals.TENOR.EXOTIC_NDO,
                com.traiana.bundle.setup.dma.DnmFinals.TENOR.BULLION,
                com.traiana.bundle.setup.dma.DnmFinals.TENOR.BULLION_SPOT_FARWARD,
                com.traiana.bundle.setup.dma.DnmFinals.TENOR.BULLION_OPTION,
                com.traiana.bundle.setup.dma.DnmFinals.TENOR.BULLION_OPTION_EXOTIC
            };
    
            for(String t : tenorsNames) {
                com.traiana.bundle.fxweb.dna.screen.ExpandChanges.BoolTextValue btv = org.mockito.Mockito.mock(com.traiana.bundle.fxweb.dna.screen.ExpandChanges.BoolTextValue.class);
                org.mockito.Mockito.when(btv.getName()).thenReturn(t);
                org.mockito.Mockito.when(btv.getValue()).thenReturn(true);
                org.mockito.Mockito.when(btv.getTextValueAsNumber()).thenReturn("20");
                org.mockito.Mockito.when(btv.getValueType()).thenReturn("D");
                tenorsAdded.add(btv);
            }
            org.mockito.Mockito.when(expandChanges.getTenorsAdded()).thenReturn(tenorsAdded);
    
            com.traiana.bundle.fxweb.dna.screen.NarrowChanges narrowChanges = org.mockito.Mockito.mock(com.traiana.bundle.fxweb.dna.screen.NarrowChanges.class);
    
            com.traiana.bundle.fxweb.dna.screen.DnaTenorByCcy dnaTenorByCcy = org.mockito.Mockito.mock(com.traiana.bundle.fxweb.dna.screen.DnaTenorByCcy.class);
            org.mockito.Mockito.when(dnaBean.getDnaTenorByCcy()).thenReturn(dnaTenorByCcy);
            
            java.util.List<com.traiana.bundle.fxweb.dna.screen.DnaTenorByCcy.TenorForCcy> tenorForCcyList = new java.util.ArrayList<com.traiana.bundle.fxweb.dna.screen.DnaTenorByCcy.TenorForCcy>();
            com.traiana.bundle.fxweb.dna.screen.DnaTenorByCcy.TenorForCcy tenorForCcy = org.mockito.Mockito.mock(com.traiana.bundle.fxweb.dna.screen.DnaTenorByCcy.TenorForCcy.class);
            org.mockito.Mockito.when(tenorForCcy.getTenor()).thenReturn("20");
            org.mockito.Mockito.when(tenorForCcy.getTimeType()).thenReturn("D");
            tenorForCcyList.add(tenorForCcy);
            org.mockito.Mockito.when(dnaTenorByCcy.getTenorForCcyList(org.mockito.Mockito.any(com.traiana.bundle.fxweb.dna.screen.DnaTenorByCcy.TenorTypeEnum.class))).thenReturn(tenorForCcyList);
    
            org.powermock.api.mockito.PowerMockito.mockStatic(com.traiana.bundle.fxweb.dna.screen.DNABean.class);
            org.mockito.Mockito.when(com.traiana.bundle.fxweb.dna.screen.DNABean.convertMaLimitTenorForEdnValue2TenorInDays(org.mockito.Mockito.anyString())).thenReturn(10.0);
            org.mockito.Mockito.when(com.traiana.bundle.fxweb.dna.screen.DNABean.tenorInDays(org.mockito.Mockito.anyString(), org.mockito.Mockito.anyString())).thenReturn(20.0);
            org.mockito.Mockito.when(com.traiana.bundle.fxweb.dna.screen.DNABean.convertMaLimitTenorForEdnValue2ArrayData(org.mockito.Mockito.anyString())).thenReturn(new String[]{"10", "D"});
    
            // Act
            org.powermock.reflect.Whitebox.invokeMethod(target, "doTenorChangesInTheDna", dnaBean, expandChanges, narrowChanges);
    
            // Assert
            org.mockito.Mockito.verify(tenor, org.mockito.Mockito.atLeastOnce()).setOverAllMaxNumber("10");
            org.mockito.Mockito.verify(tenor, org.mockito.Mockito.atLeastOnce()).setFxCashMaxNumber("10");
            org.mockito.Mockito.verify(tenor, org.mockito.Mockito.atLeastOnce()).setBullionMaxNumber("10");
            org.mockito.Mockito.verify(tenorForCcy, org.mockito.Mockito.atLeastOnce()).setTenor("10");
        }

        @Test
        public void test_doTenorChangesInTheDna_NoLimitOverride() throws Exception {
            // Arrange
            com.traiana.bundle.setup.dma.ma.MaSaveOp target = new com.traiana.bundle.setup.dma.ma.MaSaveOp();
            
            com.traiana.bundle.fxweb.dna.screen.MasterAgreementBean maBean = org.mockito.Mockito.mock(com.traiana.bundle.fxweb.dna.screen.MasterAgreementBean.class);
            org.mockito.Mockito.when(maBean.isAllowSettingTenorPLevel()).thenReturn(true);
            org.powermock.reflect.Whitebox.setInternalState(target, "maBean", maBean);
    
            com.traiana.bundle.fxweb.dna.screen.DNABean dnaBean = org.mockito.Mockito.mock(com.traiana.bundle.fxweb.dna.screen.DNABean.class);
            com.traiana.bundle.fxweb.dna.screen.DNABean.Tenor tenor = org.mockito.Mockito.mock(com.traiana.bundle.fxweb.dna.screen.DNABean.Tenor.class);
            org.mockito.Mockito.when(dnaBean.getTenor()).thenReturn(tenor);
    
            java.util.Map<String, String> maLimitTenorForEdn = org.mockito.Mockito.mock(java.util.Map.class);
            org.mockito.Mockito.when(maLimitTenorForEdn.get(org.mockito.Mockito.any(Object.class))).thenReturn("UNLIMITED");
            org.mockito.Mockito.when(dnaBean.getMaLimitTenorForEdn()).thenReturn(maLimitTenorForEdn);
    
            com.traiana.bundle.fxweb.dna.screen.ExpandChanges expandChanges = org.mockito.Mockito.mock(com.traiana.bundle.fxweb.dna.screen.ExpandChanges.class);
            java.util.List<com.traiana.bundle.fxweb.dna.screen.ExpandChanges.BoolTextValue> tenorsAdded = new java.util.ArrayList<com.traiana.bundle.fxweb.dna.screen.ExpandChanges.BoolTextValue>();
            
            String[] tenorsNames = {
                com.traiana.bundle.setup.dma.DnmFinals.TENOR.OVERALL,
                com.traiana.bundle.setup.dma.DnmFinals.TENOR.FX_CASH,
                com.traiana.bundle.setup.dma.DnmFinals.TENOR.BULLION
            };
            for(String t : tenorsNames) {
                com.traiana.bundle.fxweb.dna.screen.ExpandChanges.BoolTextValue btv = org.mockito.Mockito.mock(com.traiana.bundle.fxweb.dna.screen.ExpandChanges.BoolTextValue.class);
                org.mockito.Mockito.when(btv.getName()).thenReturn(t);
                org.mockito.Mockito.when(btv.getValue()).thenReturn(true);
                org.mockito.Mockito.when(btv.getTextValueAsNumber()).thenReturn("20");
                org.mockito.Mockito.when(btv.getValueType()).thenReturn("D");
                tenorsAdded.add(btv);
            }
            org.mockito.Mockito.when(expandChanges.getTenorsAdded()).thenReturn(tenorsAdded);
    
            com.traiana.bundle.fxweb.dna.screen.NarrowChanges narrowChanges = org.mockito.Mockito.mock(com.traiana.bundle.fxweb.dna.screen.NarrowChanges.class);
    
            com.traiana.bundle.fxweb.dna.screen.DnaTenorByCcy dnaTenorByCcy = org.mockito.Mockito.mock(com.traiana.bundle.fxweb.dna.screen.DnaTenorByCcy.class);
            org.mockito.Mockito.when(dnaBean.getDnaTenorByCcy()).thenReturn(dnaTenorByCcy);
            org.mockito.Mockito.when(dnaTenorByCcy.getTenorForCcyList(org.mockito.Mockito.any(com.traiana.bundle.fxweb.dna.screen.DnaTenorByCcy.TenorTypeEnum.class))).thenReturn(new java.util.ArrayList<com.traiana.bundle.fxweb.dna.screen.DnaTenorByCcy.TenorForCcy>());
    
            org.powermock.api.mockito.PowerMockito.mockStatic(com.traiana.bundle.fxweb.dna.screen.DNABean.class);
            org.mockito.Mockito.when(com.traiana.bundle.fxweb.dna.screen.DNABean.convertMaLimitTenorForEdnValue2TenorInDays(org.mockito.Mockito.anyString())).thenReturn(-1.0);
            org.mockito.Mockito.when(com.traiana.bundle.fxweb.dna.screen.DNABean.tenorInDays(org.mockito.Mockito.anyString(), org.mockito.Mockito.anyString())).thenReturn(20.0);
    
            // Act
            org.powermock.reflect.Whitebox.invokeMethod(target, "doTenorChangesInTheDna", dnaBean, expandChanges, narrowChanges);
    
            // Assert
            org.powermock.api.mockito.PowerMockito.verifyStatic(com.traiana.bundle.fxweb.dna.screen.DNABean.class, org.mockito.Mockito.never());
            com.traiana.bundle.fxweb.dna.screen.DNABean.convertMaLimitTenorForEdnValue2ArrayData(org.mockito.Mockito.anyString());
        }

        @Test
        public void test_insertMaTenor_validValues_success() throws Exception {
            // Arrange
            com.traiana.bundle.setup.dma.ma.MaSaveOp target = new com.traiana.bundle.setup.dma.ma.MaSaveOp();
    
            com.traiana.bundle.fxweb.dna.screen.MasterAgreementBean maBean = org.mockito.Mockito.mock(com.traiana.bundle.fxweb.dna.screen.MasterAgreementBean.class);
            org.powermock.reflect.Whitebox.setInternalState(target, "maBean", maBean);
    
            Long maId = 12345L;
            org.powermock.reflect.Whitebox.setInternalState(target, "maId", maId);
    
            Connection connection = Mockito.mock(Connection.class);
            org.powermock.reflect.Whitebox.setInternalState(target, "connection", connection);
    
            com.traiana.workspace.session.WorkspaceSession session = org.mockito.Mockito.mock(com.traiana.workspace.session.WorkspaceSession.class);
            org.powermock.reflect.Whitebox.setInternalState(target, "session", session);
    
            String userName = "testUser";
            org.powermock.reflect.Whitebox.setInternalState(target, "userName", userName);
    
            com.traiana.bundle.fxweb.dna.screen.MasterAgreementBean.Tenor tenor = org.mockito.Mockito.mock(com.traiana.bundle.fxweb.dna.screen.MasterAgreementBean.Tenor.class, new org.mockito.stubbing.Answer<Object>() {
                @Override
                public Object answer(org.mockito.invocation.InvocationOnMock invocation) throws Throwable {
                    String methodName = invocation.getMethod().getName();
                    if (methodName.startsWith("get") && (methodName.endsWith("MaxNumber") || methodName.endsWith("MinNumber"))) {
                        return "10.0";
                    }
                    if (methodName.startsWith("get") && methodName.endsWith("TypeNumber")) {
                        return "1";
                    }
                    return org.mockito.Mockito.RETURNS_DEFAULTS.answer(invocation);
                }
            });
            org.mockito.Mockito.when(maBean.getTenor()).thenReturn(tenor);
    
            org.powermock.api.mockito.PowerMockito.mockStatic(com.traiana.workspace.bean.BeansWorkspace.class);
            com.traiana.workspace.bean.BeansWorkspaceInterface workspaceMock = org.mockito.Mockito.mock(com.traiana.workspace.bean.BeansWorkspaceInterface.class);
            org.powermock.api.mockito.PowerMockito.when(com.traiana.workspace.bean.BeansWorkspace.getWorkspace()).thenReturn(workspaceMock);
    
            org.powermock.api.mockito.PowerMockito.doAnswer(new org.mockito.stubbing.Answer<Object>() {
                @Override
                public Object answer(org.mockito.invocation.InvocationOnMock invocation) throws Throwable {
                    @SuppressWarnings("unchecked")
                    java.util.Map<String, Object> params = (java.util.Map<String, Object>) invocation.getArgument(3);
                    params.put(com.traiana.workspace.bean.ServiceFinals.ID, 999L);
                    return null;
                }
            }).when(workspaceMock).doService(
                    org.mockito.Mockito.anyString(),
                    org.mockito.Mockito.anyString(),
                    org.mockito.Mockito.any(),
                    org.mockito.Mockito.any(java.util.Map.class),
                    org.mockito.Mockito.any(com.traiana.workspace.session.WorkspaceSession.class),
                    org.mockito.Mockito.anyString()
            );
    
            // Act
            org.powermock.reflect.Whitebox.invokeMethod(target, "insertMaTenor");
    
            // Assert
            org.mockito.Mockito.verify(tenor).setCashSpotFwdMinNumber(null);
            org.mockito.Mockito.verify(workspaceMock).doService(
                    org.mockito.Mockito.eq(com.traiana.workspace.bean.ServiceFinals.EDS_DNM_SERVICE_NAME),
                    org.mockito.Mockito.eq(com.traiana.workspace.bean.ServiceFinals.CMD_INSERT_MA_TENOR),
                    org.mockito.Mockito.eq(connection),
                    org.mockito.Mockito.any(java.util.Map.class),
                    org.mockito.Mockito.eq(session),
                    org.mockito.Mockito.eq(userName)
            );
            org.mockito.Mockito.verify(tenor).setId("999");
        }

        @Test
        public void test_insertMaTenor_nullValues_success() throws Exception {
            // Arrange
            com.traiana.bundle.setup.dma.ma.MaSaveOp target = new com.traiana.bundle.setup.dma.ma.MaSaveOp();
    
            com.traiana.bundle.fxweb.dna.screen.MasterAgreementBean maBean = org.mockito.Mockito.mock(com.traiana.bundle.fxweb.dna.screen.MasterAgreementBean.class);
            org.powermock.reflect.Whitebox.setInternalState(target, "maBean", maBean);
    
            Long maId = 12345L;
            org.powermock.reflect.Whitebox.setInternalState(target, "maId", maId);
    
            Connection connection = Mockito.mock(Connection.class);
            org.powermock.reflect.Whitebox.setInternalState(target, "connection", connection);
    
            com.traiana.workspace.session.WorkspaceSession session = org.mockito.Mockito.mock(com.traiana.workspace.session.WorkspaceSession.class);
            org.powermock.reflect.Whitebox.setInternalState(target, "session", session);
    
            String userName = "testUser";
            org.powermock.reflect.Whitebox.setInternalState(target, "userName", userName);
    
            com.traiana.bundle.fxweb.dna.screen.MasterAgreementBean.Tenor tenor = org.mockito.Mockito.mock(com.traiana.bundle.fxweb.dna.screen.MasterAgreementBean.Tenor.class);
            org.mockito.Mockito.when(maBean.getTenor()).thenReturn(tenor);
    
            org.powermock.api.mockito.PowerMockito.mockStatic(com.traiana.workspace.bean.BeansWorkspace.class);
            com.traiana.workspace.bean.BeansWorkspaceInterface workspaceMock = org.mockito.Mockito.mock(com.traiana.workspace.bean.BeansWorkspaceInterface.class);
            org.powermock.api.mockito.PowerMockito.when(com.traiana.workspace.bean.BeansWorkspace.getWorkspace()).thenReturn(workspaceMock);
    
            org.powermock.api.mockito.PowerMockito.doAnswer(new org.mockito.stubbing.Answer<Object>() {
                @Override
                public Object answer(org.mockito.invocation.InvocationOnMock invocation) throws Throwable {
                    @SuppressWarnings("unchecked")
                    java.util.Map<String, Object> params = (java.util.Map<String, Object>) invocation.getArgument(3);
                    params.put(com.traiana.workspace.bean.ServiceFinals.ID, 999L);
                    return null;
                }
            }).when(workspaceMock).doService(
                    org.mockito.Mockito.anyString(),
                    org.mockito.Mockito.anyString(),
                    org.mockito.Mockito.any(),
                    org.mockito.Mockito.any(java.util.Map.class),
                    org.mockito.Mockito.any(com.traiana.workspace.session.WorkspaceSession.class),
                    org.mockito.Mockito.anyString()
            );
    
            // Act
            org.powermock.reflect.Whitebox.invokeMethod(target, "insertMaTenor");
    
            // Assert
            org.mockito.Mockito.verify(tenor).setCashSpotFwdMinNumber(null);
            org.mockito.Mockito.verify(workspaceMock).doService(
                    org.mockito.Mockito.eq(com.traiana.workspace.bean.ServiceFinals.EDS_DNM_SERVICE_NAME),
                    org.mockito.Mockito.eq(com.traiana.workspace.bean.ServiceFinals.CMD_INSERT_MA_TENOR),
                    org.mockito.Mockito.eq(connection),
                    org.mockito.Mockito.any(java.util.Map.class),
                    org.mockito.Mockito.eq(session),
                    org.mockito.Mockito.eq(userName)
            );
            org.mockito.Mockito.verify(tenor).setId("999");
        }

        @Test
        public void test_insertMaTenor_nullTenor_throwsException() throws Exception {
            // Arrange
            com.traiana.bundle.setup.dma.ma.MaSaveOp target = new com.traiana.bundle.setup.dma.ma.MaSaveOp();
            com.traiana.bundle.fxweb.dna.screen.MasterAgreementBean maBean = org.mockito.Mockito.mock(com.traiana.bundle.fxweb.dna.screen.MasterAgreementBean.class);
            org.powermock.reflect.Whitebox.setInternalState(target, "maBean", maBean);
            org.mockito.Mockito.when(maBean.getTenor()).thenReturn(null);
    
            // Act
            try {
                org.powermock.reflect.Whitebox.invokeMethod(target, "insertMaTenor");
                org.junit.Assert.fail("Expected an exception due to null tenor");
            } catch (Exception e) {
                // Assert
                boolean isNpe = e instanceof NullPointerException || (e.getCause() != null && e.getCause() instanceof NullPointerException);
                org.junit.Assert.assertTrue("Exception should be or wrap a NullPointerException", isNpe);
            }
        }

        @Test
        public void test_populateExpandChangesProductAdded_AllTrue() throws Exception {
            // Arrange
            MaSaveOp target = PowerMockito.spy(new MaSaveOp());
    
            MasterAgreementBean oldMasterAgreementBean = Mockito.mock(MasterAgreementBean.class);
            ExpandChanges expandChanges = Mockito.mock(ExpandChanges.class);
            MaLimitTenorForEdnBean maLimitTenorForEdnBean = Mockito.mock(MaLimitTenorForEdnBean.class);
    
            MasterAgreementBean maBean = Mockito.mock(MasterAgreementBean.class);
            MasterAgreementBean.Tenor tenor = Mockito.mock(MasterAgreementBean.Tenor.class);
            Mockito.when(maBean.getTenor()).thenReturn(tenor);
            Whitebox.setInternalState(target, "maBean", maBean);
    
            Map mapInDays = Mockito.mock(Map.class);
            Mockito.when(mapInDays.get(Mockito.any())).thenReturn(1.0);
            Mockito.when(maLimitTenorForEdnBean.getMaLimitTenorInDays()).thenReturn(mapInDays);
    
            Map mapForEdn = Mockito.mock(Map.class);
            Mockito.when(mapForEdn.get(Mockito.any())).thenReturn("str");
            Mockito.when(maLimitTenorForEdnBean.getMaLimitTenorForEdn()).thenReturn(mapForEdn);
    
            Mockito.when(maBean.isFxCash()).thenReturn(true);
            Mockito.when(maBean.isSpotFarward()).thenReturn(true);
            Mockito.when(maBean.isFarward()).thenReturn(true);
            Mockito.when(maBean.isNdf()).thenReturn(true);
            Mockito.when(maBean.isFxOptions()).thenReturn(true);
            Mockito.when(maBean.isVanillaOptions()).thenReturn(true);
            Mockito.when(maBean.isEuropeanOptions()).thenReturn(true);
            Mockito.when(maBean.isAmericanOptions()).thenReturn(true);
            Mockito.when(maBean.isNdoOptions()).thenReturn(true);
            Mockito.when(maBean.isExoticOption()).thenReturn(true);
            Mockito.when(maBean.isSingleBarrierOptions()).thenReturn(true);
            Mockito.when(maBean.isDoubleBarrierOptions()).thenReturn(true);
            Mockito.when(maBean.isDigitalOptions()).thenReturn(true);
            Mockito.when(maBean.isEuropeanDigital()).thenReturn(true);
            Mockito.when(maBean.isEuropeanDigitalWithKnockIn()).thenReturn(true);
            Mockito.when(maBean.isEuropeanDigitalWithKnockOut()).thenReturn(true);
            Mockito.when(maBean.isOneTouchPayoutAtMaturity()).thenReturn(true);
            Mockito.when(maBean.isOneTouchInstantPayout()).thenReturn(true);
            Mockito.when(maBean.isNoTouch()).thenReturn(true);
            Mockito.when(maBean.isDoubleOneTouch()).thenReturn(true);
            Mockito.when(maBean.isDoubleNoTouch()).thenReturn(true);
            Mockito.when(maBean.isWindowSingleBarrierOptions()).thenReturn(true);
            Mockito.when(maBean.isWindowDoubleBarrierOptions()).thenReturn(true);
            Mockito.when(maBean.isBasketOptions()).thenReturn(true);
            Mockito.when(maBean.isMultiLegOptions()).thenReturn(true);
            Mockito.when(maBean.isAsianOptions()).thenReturn(true);
            Mockito.when(maBean.isBermudanOptions()).thenReturn(true);
            Mockito.when(maBean.isExoticNdo()).thenReturn(true);
            Mockito.when(maBean.isBullion()).thenReturn(true);
            Mockito.when(maBean.isBullionSpotFarward()).thenReturn(true);
            Mockito.when(maBean.isBullionOption()).thenReturn(true);
            Mockito.when(maBean.isBullionOptionExotic()).thenReturn(true);
    
            PowerMockito.doReturn("desc").when(target, "generateTenorDescriptionForProductAdded", Mockito.anyString(), Mockito.anyString());
            PowerMockito.doReturn(10.0).when(target, "culculateMaxNumberInDays", Mockito.any(Double.class));
    
            // Act
            Whitebox.invokeMethod(target, "populateExpandChangesProductAdded", oldMasterAgreementBean, expandChanges, maLimitTenorForEdnBean);
    
            // Assert
            Mockito.verify(expandChanges, Mockito.atLeast(30)).addProduct(
                Mockito.anyString(), Mockito.anyBoolean(), Mockito.anyString(), 
                Mockito.anyString(), Mockito.anyString(), Mockito.anyDouble()
            );
        }

        @Test
        public void test_populateExpandChangesProductAdded_AllFalse() throws Exception {
            // Arrange
            MaSaveOp target = PowerMockito.spy(new MaSaveOp());
    
            MasterAgreementBean oldMasterAgreementBean = Mockito.mock(MasterAgreementBean.class);
            ExpandChanges expandChanges = Mockito.mock(ExpandChanges.class);
            MaLimitTenorForEdnBean maLimitTenorForEdnBean = Mockito.mock(MaLimitTenorForEdnBean.class);
    
            MasterAgreementBean maBean = Mockito.mock(MasterAgreementBean.class);
            MasterAgreementBean.Tenor tenor = Mockito.mock(MasterAgreementBean.Tenor.class);
            Mockito.when(maBean.getTenor()).thenReturn(tenor);
            Whitebox.setInternalState(target, "maBean", maBean);
    
            Map mapInDays = Mockito.mock(Map.class);
            Mockito.when(maLimitTenorForEdnBean.getMaLimitTenorInDays()).thenReturn(mapInDays);
    
            Map mapForEdn = Mockito.mock(Map.class);
            Mockito.when(maLimitTenorForEdnBean.getMaLimitTenorForEdn()).thenReturn(mapForEdn);
    
            // Act
            Whitebox.invokeMethod(target, "populateExpandChangesProductAdded", oldMasterAgreementBean, expandChanges, maLimitTenorForEdnBean);
    
            // Assert
            Mockito.verifyZeroInteractions(expandChanges);
        }

        @Test
        public void test_populateExpandChangesProductAdded_OldTrue() throws Exception {
            // Arrange
            MaSaveOp target = PowerMockito.spy(new MaSaveOp());
    
            MasterAgreementBean oldMasterAgreementBean = Mockito.mock(MasterAgreementBean.class);
            ExpandChanges expandChanges = Mockito.mock(ExpandChanges.class);
            MaLimitTenorForEdnBean maLimitTenorForEdnBean = Mockito.mock(MaLimitTenorForEdnBean.class);
    
            MasterAgreementBean maBean = Mockito.mock(MasterAgreementBean.class);
            MasterAgreementBean.Tenor tenor = Mockito.mock(MasterAgreementBean.Tenor.class);
            Mockito.when(maBean.getTenor()).thenReturn(tenor);
            Whitebox.setInternalState(target, "maBean", maBean);
    
            Map mapInDays = Mockito.mock(Map.class);
            Mockito.when(maLimitTenorForEdnBean.getMaLimitTenorInDays()).thenReturn(mapInDays);
    
            Map mapForEdn = Mockito.mock(Map.class);
            Mockito.when(maLimitTenorForEdnBean.getMaLimitTenorForEdn()).thenReturn(mapForEdn);
    
            Mockito.when(oldMasterAgreementBean.isFxCash()).thenReturn(true);
            Mockito.when(oldMasterAgreementBean.isSpotFarward()).thenReturn(true);
            Mockito.when(oldMasterAgreementBean.isFarward()).thenReturn(true);
            Mockito.when(oldMasterAgreementBean.isNdf()).thenReturn(true);
            Mockito.when(oldMasterAgreementBean.isFxOptions()).thenReturn(true);
            Mockito.when(oldMasterAgreementBean.isVanillaOptions()).thenReturn(true);
            Mockito.when(oldMasterAgreementBean.isEuropeanOptions()).thenReturn(true);
            Mockito.when(oldMasterAgreementBean.isAmericanOptions()).thenReturn(true);
            Mockito.when(oldMasterAgreementBean.isNdoOptions()).thenReturn(true);
            Mockito.when(oldMasterAgreementBean.isExoticOption()).thenReturn(true);
            Mockito.when(oldMasterAgreementBean.isSingleBarrierOptions()).thenReturn(true);
            Mockito.when(oldMasterAgreementBean.isDoubleBarrierOptions()).thenReturn(true);
            Mockito.when(oldMasterAgreementBean.isDigitalOptions()).thenReturn(true);
            Mockito.when(oldMasterAgreementBean.isEuropeanDigital()).thenReturn(true);
            Mockito.when(oldMasterAgreementBean.isEuropeanDigitalWithKnockIn()).thenReturn(true);
            Mockito.when(oldMasterAgreementBean.isEuropeanDigitalWithKnockOut()).thenReturn(true);
            Mockito.when(oldMasterAgreementBean.isOneTouchPayoutAtMaturity()).thenReturn(true);
            Mockito.when(oldMasterAgreementBean.isOneTouchInstantPayout()).thenReturn(true);
            Mockito.when(oldMasterAgreementBean.isNoTouch()).thenReturn(true);
            Mockito.when(oldMasterAgreementBean.isDoubleOneTouch()).thenReturn(true);
            Mockito.when(oldMasterAgreementBean.isDoubleNoTouch()).thenReturn(true);
            Mockito.when(oldMasterAgreementBean.isWindowSingleBarrierOptions()).thenReturn(true);
            Mockito.when(oldMasterAgreementBean.isWindowDoubleBarrierOptions()).thenReturn(true);
            Mockito.when(oldMasterAgreementBean.isBasketOptions()).thenReturn(true);
            Mockito.when(oldMasterAgreementBean.isMultiLegOptions()).thenReturn(true);
            Mockito.when(oldMasterAgreementBean.isAsianOptions()).thenReturn(true);
            Mockito.when(oldMasterAgreementBean.isBermudanOptions()).thenReturn(true);
            Mockito.when(oldMasterAgreementBean.isExoticNdo()).thenReturn(true);
            Mockito.when(oldMasterAgreementBean.isBullion()).thenReturn(true);
            Mockito.when(oldMasterAgreementBean.isBullionSpotFarward()).thenReturn(true);
            Mockito.when(oldMasterAgreementBean.isBullionOption()).thenReturn(true);
            Mockito.when(oldMasterAgreementBean.isBullionOptionExotic()).thenReturn(true);
    
            Mockito.when(maBean.isFxCash()).thenReturn(true);
            Mockito.when(maBean.isSpotFarward()).thenReturn(true);
            Mockito.when(maBean.isFarward()).thenReturn(true);
            Mockito.when(maBean.isNdf()).thenReturn(true);
            Mockito.when(maBean.isFxOptions()).thenReturn(true);
            Mockito.when(maBean.isVanillaOptions()).thenReturn(true);
            Mockito.when(maBean.isEuropeanOptions()).thenReturn(true);
            Mockito.when(maBean.isAmericanOptions()).thenReturn(true);
            Mockito.when(maBean.isNdoOptions()).thenReturn(true);
            Mockito.when(maBean.isExoticOption()).thenReturn(true);
            Mockito.when(maBean.isSingleBarrierOptions()).thenReturn(true);
            Mockito.when(maBean.isDoubleBarrierOptions()).thenReturn(true);
            Mockito.when(maBean.isDigitalOptions()).thenReturn(true);
            Mockito.when(maBean.isEuropeanDigital()).thenReturn(true);
            Mockito.when(maBean.isEuropeanDigitalWithKnockIn()).thenReturn(true);
            Mockito.when(maBean.isEuropeanDigitalWithKnockOut()).thenReturn(true);
            Mockito.when(maBean.isOneTouchPayoutAtMaturity()).thenReturn(true);
            Mockito.when(maBean.isOneTouchInstantPayout()).thenReturn(true);
            Mockito.when(maBean.isNoTouch()).thenReturn(true);
            Mockito.when(maBean.isDoubleOneTouch()).thenReturn(true);
            Mockito.when(maBean.isDoubleNoTouch()).thenReturn(true);
            Mockito.when(maBean.isWindowSingleBarrierOptions()).thenReturn(true);
            Mockito.when(maBean.isWindowDoubleBarrierOptions()).thenReturn(true);
            Mockito.when(maBean.isBasketOptions()).thenReturn(true);
            Mockito.when(maBean.isMultiLegOptions()).thenReturn(true);
            Mockito.when(maBean.isAsianOptions()).thenReturn(true);
            Mockito.when(maBean.isBermudanOptions()).thenReturn(true);
            Mockito.when(maBean.isExoticNdo()).thenReturn(true);
            Mockito.when(maBean.isBullion()).thenReturn(true);
            Mockito.when(maBean.isBullionSpotFarward()).thenReturn(true);
            Mockito.when(maBean.isBullionOption()).thenReturn(true);
            Mockito.when(maBean.isBullionOptionExotic()).thenReturn(true);
    
            // Act
            Whitebox.invokeMethod(target, "populateExpandChangesProductAdded", oldMasterAgreementBean, expandChanges, maLimitTenorForEdnBean);
    
            // Assert
            Mockito.verifyZeroInteractions(expandChanges);
        }

        @Test
        public void test_populateExpandChangesProductAdded_NotNullValues() throws Exception {
            // Arrange
            MaSaveOp target = PowerMockito.spy(new MaSaveOp());
    
            MasterAgreementBean oldMasterAgreementBean = Mockito.mock(MasterAgreementBean.class);
            ExpandChanges expandChanges = Mockito.mock(ExpandChanges.class);
            MaLimitTenorForEdnBean maLimitTenorForEdnBean = Mockito.mock(MaLimitTenorForEdnBean.class);
    
            MasterAgreementBean maBean = Mockito.mock(MasterAgreementBean.class);
            MasterAgreementBean.Tenor tenor = Mockito.mock(MasterAgreementBean.Tenor.class);
            Mockito.when(maBean.getTenor()).thenReturn(tenor);
            Whitebox.setInternalState(target, "maBean", maBean);
    
            Mockito.when(tenor.getCashSpotFwdMinNumber()).thenReturn("1");
    
            Map mapInDays = Mockito.mock(Map.class);
            Mockito.when(mapInDays.get(Mockito.any())).thenReturn(1.0);
            Mockito.when(maLimitTenorForEdnBean.getMaLimitTenorInDays()).thenReturn(mapInDays);
    
            Map mapForEdn = Mockito.mock(Map.class);
            Mockito.when(mapForEdn.get(Mockito.any())).thenReturn("str");
            Mockito.when(maLimitTenorForEdnBean.getMaLimitTenorForEdn()).thenReturn(mapForEdn);
    
            Mockito.when(maBean.isSpotFarward()).thenReturn(true);
    
            PowerMockito.doReturn("desc").when(target, "generateTenorDescriptionForProductAdded", Mockito.anyString(), Mockito.anyString());
            PowerMockito.doReturn(10.0).when(target, "culculateMaxNumberInDays", Mockito.any(Double.class));
    
            // Act
            Whitebox.invokeMethod(target, "populateExpandChangesProductAdded", oldMasterAgreementBean, expandChanges, maLimitTenorForEdnBean);
    
            // Assert
            Mockito.verify(expandChanges, Mockito.atLeastOnce()).addProduct(
                Mockito.anyString(), Mockito.anyBoolean(), Mockito.anyString(), 
                Mockito.anyString(), Mockito.anyString(), Mockito.anyDouble()
            );
        }

        @Test
        public void test_populateProductNarrowsSB_allRemoved() throws Exception {
            // Arrange
            com.traiana.bundle.setup.dma.ma.MaSaveOp target = new com.traiana.bundle.setup.dma.ma.MaSaveOp();
            com.traiana.bundle.fxweb.dna.screen.MasterAgreementBean oldMaBean = org.mockito.Mockito.mock(com.traiana.bundle.fxweb.dna.screen.MasterAgreementBean.class);
            com.traiana.bundle.fxweb.dna.screen.MasterAgreementBean maBean = org.mockito.Mockito.mock(com.traiana.bundle.fxweb.dna.screen.MasterAgreementBean.class);
            
            org.powermock.reflect.Whitebox.setInternalState(target, "maBean", maBean);
    
            org.mockito.Mockito.when(oldMaBean.isFxCash()).thenReturn(true);
            org.mockito.Mockito.when(oldMaBean.isSpotFarward()).thenReturn(true);
            org.mockito.Mockito.when(oldMaBean.isFarward()).thenReturn(true);
            org.mockito.Mockito.when(oldMaBean.isNdf()).thenReturn(true);
            org.mockito.Mockito.when(oldMaBean.isFxOptions()).thenReturn(true);
            org.mockito.Mockito.when(oldMaBean.isVanillaOptions()).thenReturn(true);
            org.mockito.Mockito.when(oldMaBean.isEuropeanOptions()).thenReturn(true);
            org.mockito.Mockito.when(oldMaBean.isAmericanOptions()).thenReturn(true);
            org.mockito.Mockito.when(oldMaBean.isNdoOptions()).thenReturn(true);
            org.mockito.Mockito.when(oldMaBean.isExoticOption()).thenReturn(true);
            org.mockito.Mockito.when(oldMaBean.isMultiLegOptions()).thenReturn(true);
            org.mockito.Mockito.when(oldMaBean.isSingleBarrierOptions()).thenReturn(true);
            org.mockito.Mockito.when(oldMaBean.isDoubleBarrierOptions()).thenReturn(true);
            org.mockito.Mockito.when(oldMaBean.isDigitalOptions()).thenReturn(true);
            org.mockito.Mockito.when(oldMaBean.isEuropeanDigital()).thenReturn(true);
            org.mockito.Mockito.when(oldMaBean.isEuropeanDigitalWithKnockIn()).thenReturn(true);
            org.mockito.Mockito.when(oldMaBean.isEuropeanDigitalWithKnockOut()).thenReturn(true);
            org.mockito.Mockito.when(oldMaBean.isOneTouchPayoutAtMaturity()).thenReturn(true);
            org.mockito.Mockito.when(oldMaBean.isOneTouchInstantPayout()).thenReturn(true);
            org.mockito.Mockito.when(oldMaBean.isNoTouch()).thenReturn(true);
            org.mockito.Mockito.when(oldMaBean.isDoubleOneTouch()).thenReturn(true);
            org.mockito.Mockito.when(oldMaBean.isDoubleNoTouch()).thenReturn(true);
            org.mockito.Mockito.when(oldMaBean.isWindowSingleBarrierOptions()).thenReturn(true);
            org.mockito.Mockito.when(oldMaBean.isWindowDoubleBarrierOptions()).thenReturn(true);
            org.mockito.Mockito.when(oldMaBean.isBasketOptions()).thenReturn(true);
            org.mockito.Mockito.when(oldMaBean.isAsianOptions()).thenReturn(true);
            org.mockito.Mockito.when(oldMaBean.isBermudanOptions()).thenReturn(true);
            org.mockito.Mockito.when(oldMaBean.isExoticNdo()).thenReturn(true);
            org.mockito.Mockito.when(oldMaBean.isBullion()).thenReturn(true);
            org.mockito.Mockito.when(oldMaBean.isBullionSpotFarward()).thenReturn(true);
            org.mockito.Mockito.when(oldMaBean.isBullionOption()).thenReturn(true);
            org.mockito.Mockito.when(oldMaBean.isBullionOptionExotic()).thenReturn(true);
            
            org.mockito.Mockito.when(oldMaBean.getAdditionalProductsSelectedList()).thenReturn(java.util.Arrays.asList("ProdA", "ProdB"));
            org.mockito.Mockito.when(maBean.getAdditionalProductsSelectedList()).thenReturn(java.util.Collections.singletonList("ProdA"));
    
            java.lang.StringBuilder narrowChangeSB = new java.lang.StringBuilder();
            com.traiana.bundle.fxweb.dna.screen.NarrowChanges narrowChanges = org.mockito.Mockito.mock(com.traiana.bundle.fxweb.dna.screen.NarrowChanges.class);
    
            // Act
            org.powermock.reflect.Whitebox.invokeMethod(target, "populateProductNarrowsSB", oldMaBean, narrowChangeSB, narrowChanges);
    
            // Assert
            org.mockito.Mockito.verify(narrowChanges, org.mockito.Mockito.times(33)).addProduct(org.mockito.Mockito.anyString());
            org.junit.Assert.assertTrue(narrowChangeSB.toString().contains("Product Removed"));
        }

        @Test
        public void test_populateProductNarrowsSB_noChanges_bothFalse() throws Exception {
            // Arrange
            com.traiana.bundle.setup.dma.ma.MaSaveOp target = new com.traiana.bundle.setup.dma.ma.MaSaveOp();
            com.traiana.bundle.fxweb.dna.screen.MasterAgreementBean oldMaBean = org.mockito.Mockito.mock(com.traiana.bundle.fxweb.dna.screen.MasterAgreementBean.class);
            com.traiana.bundle.fxweb.dna.screen.MasterAgreementBean maBean = org.mockito.Mockito.mock(com.traiana.bundle.fxweb.dna.screen.MasterAgreementBean.class);
            
            org.powermock.reflect.Whitebox.setInternalState(target, "maBean", maBean);
    
            org.mockito.Mockito.when(oldMaBean.getAdditionalProductsSelectedList()).thenReturn(java.util.Collections.<String>emptyList());
            org.mockito.Mockito.when(maBean.getAdditionalProductsSelectedList()).thenReturn(java.util.Collections.<String>emptyList());
    
            java.lang.StringBuilder narrowChangeSB = new java.lang.StringBuilder();
            com.traiana.bundle.fxweb.dna.screen.NarrowChanges narrowChanges = org.mockito.Mockito.mock(com.traiana.bundle.fxweb.dna.screen.NarrowChanges.class);
    
            // Act
            org.powermock.reflect.Whitebox.invokeMethod(target, "populateProductNarrowsSB", oldMaBean, narrowChangeSB, narrowChanges);
    
            // Assert
            org.mockito.Mockito.verifyZeroInteractions(narrowChanges);
            org.junit.Assert.assertEquals(0, narrowChangeSB.length());
        }

        @Test
        public void test_populateProductNarrowsSB_noChanges_bothTrue() throws Exception {
            // Arrange
            com.traiana.bundle.setup.dma.ma.MaSaveOp target = new com.traiana.bundle.setup.dma.ma.MaSaveOp();
            com.traiana.bundle.fxweb.dna.screen.MasterAgreementBean oldMaBean = org.mockito.Mockito.mock(com.traiana.bundle.fxweb.dna.screen.MasterAgreementBean.class);
            com.traiana.bundle.fxweb.dna.screen.MasterAgreementBean maBean = org.mockito.Mockito.mock(com.traiana.bundle.fxweb.dna.screen.MasterAgreementBean.class);
            
            org.powermock.reflect.Whitebox.setInternalState(target, "maBean", maBean);
    
            org.mockito.Mockito.when(oldMaBean.isFxCash()).thenReturn(true);
            org.mockito.Mockito.when(oldMaBean.isSpotFarward()).thenReturn(true);
            org.mockito.Mockito.when(oldMaBean.isFarward()).thenReturn(true);
            org.mockito.Mockito.when(oldMaBean.isNdf()).thenReturn(true);
            org.mockito.Mockito.when(oldMaBean.isFxOptions()).thenReturn(true);
            org.mockito.Mockito.when(oldMaBean.isVanillaOptions()).thenReturn(true);
            org.mockito.Mockito.when(oldMaBean.isEuropeanOptions()).thenReturn(true);
            org.mockito.Mockito.when(oldMaBean.isAmericanOptions()).thenReturn(true);
            org.mockito.Mockito.when(oldMaBean.isNdoOptions()).thenReturn(true);
            org.mockito.Mockito.when(oldMaBean.isExoticOption()).thenReturn(true);
            org.mockito.Mockito.when(oldMaBean.isMultiLegOptions()).thenReturn(true);
            org.mockito.Mockito.when(oldMaBean.isSingleBarrierOptions()).thenReturn(true);
            org.mockito.Mockito.when(oldMaBean.isDoubleBarrierOptions()).thenReturn(true);
            org.mockito.Mockito.when(oldMaBean.isDigitalOptions()).thenReturn(true);
            org.mockito.Mockito.when(oldMaBean.isEuropeanDigital()).thenReturn(true);
            org.mockito.Mockito.when(oldMaBean.isEuropeanDigitalWithKnockIn()).thenReturn(true);
            org.mockito.Mockito.when(oldMaBean.isEuropeanDigitalWithKnockOut()).thenReturn(true);
            org.mockito.Mockito.when(oldMaBean.isOneTouchPayoutAtMaturity()).thenReturn(true);
            org.mockito.Mockito.when(oldMaBean.isOneTouchInstantPayout()).thenReturn(true);
            org.mockito.Mockito.when(oldMaBean.isNoTouch()).thenReturn(true);
            org.mockito.Mockito.when(oldMaBean.isDoubleOneTouch()).thenReturn(true);
            org.mockito.Mockito.when(oldMaBean.isDoubleNoTouch()).thenReturn(true);
            org.mockito.Mockito.when(oldMaBean.isWindowSingleBarrierOptions()).thenReturn(true);
            org.mockito.Mockito.when(oldMaBean.isWindowDoubleBarrierOptions()).thenReturn(true);
            org.mockito.Mockito.when(oldMaBean.isBasketOptions()).thenReturn(true);
            org.mockito.Mockito.when(oldMaBean.isAsianOptions()).thenReturn(true);
            org.mockito.Mockito.when(oldMaBean.isBermudanOptions()).thenReturn(true);
            org.mockito.Mockito.when(oldMaBean.isExoticNdo()).thenReturn(true);
            org.mockito.Mockito.when(oldMaBean.isBullion()).thenReturn(true);
            org.mockito.Mockito.when(oldMaBean.isBullionSpotFarward()).thenReturn(true);
            org.mockito.Mockito.when(oldMaBean.isBullionOption()).thenReturn(true);
            org.mockito.Mockito.when(oldMaBean.isBullionOptionExotic()).thenReturn(true);
            
            org.mockito.Mockito.when(maBean.isFxCash()).thenReturn(true);
            org.mockito.Mockito.when(maBean.isSpotFarward()).thenReturn(true);
            org.mockito.Mockito.when(maBean.isFarward()).thenReturn(true);
            org.mockito.Mockito.when(maBean.isNdf()).thenReturn(true);
            org.mockito.Mockito.when(maBean.isFxOptions()).thenReturn(true);
            org.mockito.Mockito.when(maBean.isVanillaOptions()).thenReturn(true);
            org.mockito.Mockito.when(maBean.isEuropeanOptions()).thenReturn(true);
            org.mockito.Mockito.when(maBean.isAmericanOptions()).thenReturn(true);
            org.mockito.Mockito.when(maBean.isNdoOptions()).thenReturn(true);
            org.mockito.Mockito.when(maBean.isExoticOption()).thenReturn(true);
            org.mockito.Mockito.when(maBean.isMultiLegOptions()).thenReturn(true);
            org.mockito.Mockito.when(maBean.isSingleBarrierOptions()).thenReturn(true);
            org.mockito.Mockito.when(maBean.isDoubleBarrierOptions()).thenReturn(true);
            org.mockito.Mockito.when(maBean.isDigitalOptions()).thenReturn(true);
            org.mockito.Mockito.when(maBean.isEuropeanDigital()).thenReturn(true);
            org.mockito.Mockito.when(maBean.isEuropeanDigitalWithKnockIn()).thenReturn(true);
            org.mockito.Mockito.when(maBean.isEuropeanDigitalWithKnockOut()).thenReturn(true);
            org.mockito.Mockito.when(maBean.isOneTouchPayoutAtMaturity()).thenReturn(true);
            org.mockito.Mockito.when(maBean.isOneTouchInstantPayout()).thenReturn(true);
            org.mockito.Mockito.when(maBean.isNoTouch()).thenReturn(true);
            org.mockito.Mockito.when(maBean.isDoubleOneTouch()).thenReturn(true);
            org.mockito.Mockito.when(maBean.isDoubleNoTouch()).thenReturn(true);
            org.mockito.Mockito.when(maBean.isWindowSingleBarrierOptions()).thenReturn(true);
            org.mockito.Mockito.when(maBean.isWindowDoubleBarrierOptions()).thenReturn(true);
            org.mockito.Mockito.when(maBean.isBasketOptions()).thenReturn(true);
            org.mockito.Mockito.when(maBean.isAsianOptions()).thenReturn(true);
            org.mockito.Mockito.when(maBean.isBermudanOptions()).thenReturn(true);
            org.mockito.Mockito.when(maBean.isExoticNdo()).thenReturn(true);
            org.mockito.Mockito.when(maBean.isBullion()).thenReturn(true);
            org.mockito.Mockito.when(maBean.isBullionSpotFarward()).thenReturn(true);
            org.mockito.Mockito.when(maBean.isBullionOption()).thenReturn(true);
            org.mockito.Mockito.when(maBean.isBullionOptionExotic()).thenReturn(true);
    
            org.mockito.Mockito.when(oldMaBean.getAdditionalProductsSelectedList()).thenReturn(java.util.Arrays.asList("ProdA"));
            org.mockito.Mockito.when(maBean.getAdditionalProductsSelectedList()).thenReturn(java.util.Arrays.asList("ProdA"));
    
            java.lang.StringBuilder narrowChangeSB = new java.lang.StringBuilder();
            com.traiana.bundle.fxweb.dna.screen.NarrowChanges narrowChanges = org.mockito.Mockito.mock(com.traiana.bundle.fxweb.dna.screen.NarrowChanges.class);
    
            // Act
            org.powermock.reflect.Whitebox.invokeMethod(target, "populateProductNarrowsSB", oldMaBean, narrowChangeSB, narrowChanges);
    
            // Assert
            org.mockito.Mockito.verifyZeroInteractions(narrowChanges);
            org.junit.Assert.assertEquals(0, narrowChangeSB.length());
        }

        @Test
        public void test_createOldMaTenor_null2MaxValue_AllMainsTrue_NullLimits() throws Exception {
            // Arrange
            com.traiana.bundle.setup.dma.ma.MaSaveOp target = new com.traiana.bundle.setup.dma.ma.MaSaveOp();
            com.traiana.bundle.fxweb.dna.screen.MasterAgreementBean oldMasterAgreementBean = Mockito.mock(com.traiana.bundle.fxweb.dna.screen.MasterAgreementBean.class);
            com.traiana.bundle.fxweb.dna.screen.MasterAgreementBean.Tenor oldMaTenor = Mockito.mock(com.traiana.bundle.fxweb.dna.screen.MasterAgreementBean.Tenor.class);
            com.traiana.bundle.fxweb.dna.screen.MasterAgreementBean.Tenor clonedTenor = Mockito.mock(com.traiana.bundle.fxweb.dna.screen.MasterAgreementBean.Tenor.class);
            
            Mockito.when(oldMaTenor.clone()).thenReturn(clonedTenor);
            Mockito.when(oldMasterAgreementBean.isFxCash()).thenReturn(true);
            Mockito.when(oldMasterAgreementBean.isFxOptions()).thenReturn(true);
            Mockito.when(oldMasterAgreementBean.isBullion()).thenReturn(true);
            
            // Act
            com.traiana.bundle.fxweb.dna.screen.MasterAgreementBean.Tenor result = Whitebox.invokeMethod(target, "createOldMaTenor_null2MaxValue", oldMasterAgreementBean, oldMaTenor);
            
            // Assert
            org.junit.Assert.assertSame(clonedTenor, result);
            String maxVal = String.valueOf(Double.MAX_VALUE);
            
            Mockito.verify(clonedTenor).setFxCashMaxNumber(maxVal);
            Mockito.verify(clonedTenor).setCashSpotFwdMaxNumber(maxVal);
            Mockito.verify(clonedTenor).setNdfMaxNumber(maxVal);
            Mockito.verify(clonedTenor).setFxOptionMaxNumber(maxVal);
            Mockito.verify(clonedTenor).setVanillaMaxNumber(maxVal);
            Mockito.verify(clonedTenor).setVanillaDeliverableMaxNumber(maxVal);
            Mockito.verify(clonedTenor).setExoticOptionMaxNumber(maxVal);
            Mockito.verify(clonedTenor).setNdoMaxNumber(maxVal);
            Mockito.verify(clonedTenor).setMultiLegOptionMaxNumber(maxVal);
            Mockito.verify(clonedTenor).setSingleBarrierMaxNumber(maxVal);
            Mockito.verify(clonedTenor).setDoubleBarrierMaxNumber(maxVal);
            Mockito.verify(clonedTenor).setDigitalMaxNumber(maxVal);
            Mockito.verify(clonedTenor).setBasketMaxNumber(maxVal);
            Mockito.verify(clonedTenor).setAsianOptionMaxNumber(maxVal);
            Mockito.verify(clonedTenor).setBermudanOptionMaxNumber(maxVal);
            Mockito.verify(clonedTenor).setExoticNdoMaxNumber(maxVal);
            Mockito.verify(clonedTenor).setBullionMaxNumber(maxVal);
            Mockito.verify(clonedTenor).setBullionSpotFwdMaxNumber(maxVal);
            Mockito.verify(clonedTenor).setBullionOptionsMaxNumber(maxVal);
            Mockito.verify(clonedTenor).setBullionOptionsExoticMaxNumber(maxVal);
        }

        @Test
        public void test_createOldMaTenor_null2MaxValue_AllMainsFalse_SubsTrue_NullLimits() throws Exception {
            // Arrange
            com.traiana.bundle.setup.dma.ma.MaSaveOp target = new com.traiana.bundle.setup.dma.ma.MaSaveOp();
            com.traiana.bundle.fxweb.dna.screen.MasterAgreementBean oldMasterAgreementBean = Mockito.mock(com.traiana.bundle.fxweb.dna.screen.MasterAgreementBean.class);
            com.traiana.bundle.fxweb.dna.screen.MasterAgreementBean.Tenor oldMaTenor = Mockito.mock(com.traiana.bundle.fxweb.dna.screen.MasterAgreementBean.Tenor.class);
            com.traiana.bundle.fxweb.dna.screen.MasterAgreementBean.Tenor clonedTenor = Mockito.mock(com.traiana.bundle.fxweb.dna.screen.MasterAgreementBean.Tenor.class);
            
            Mockito.when(oldMaTenor.clone()).thenReturn(clonedTenor);
            Mockito.when(oldMasterAgreementBean.isFxCash()).thenReturn(false);
            Mockito.when(oldMasterAgreementBean.isSpotFarward()).thenReturn(false);
            Mockito.when(oldMasterAgreementBean.isFarward()).thenReturn(true);
            Mockito.when(oldMasterAgreementBean.isNdf()).thenReturn(true);
            
            Mockito.when(oldMasterAgreementBean.isFxOptions()).thenReturn(false);
            Mockito.when(oldMasterAgreementBean.isVanillaOptionsFamily()).thenReturn(true);
            Mockito.when(oldMasterAgreementBean.isVanillaDeliverableFamily()).thenReturn(true);
            Mockito.when(oldMasterAgreementBean.isNdoOptions()).thenReturn(true);
            Mockito.when(oldMasterAgreementBean.isExoticOptionFamily()).thenReturn(true);
            Mockito.when(oldMasterAgreementBean.isMultiLegOptions()).thenReturn(true);
            Mockito.when(oldMasterAgreementBean.isSingleBarrierOptions()).thenReturn(true);
            Mockito.when(oldMasterAgreementBean.isDoubleBarrierOptions()).thenReturn(true);
            Mockito.when(oldMasterAgreementBean.isDigitalOptionsFamily()).thenReturn(true);
            Mockito.when(oldMasterAgreementBean.isBasketOptions()).thenReturn(true);
            Mockito.when(oldMasterAgreementBean.isAsianOptions()).thenReturn(true);
            Mockito.when(oldMasterAgreementBean.isBermudanOptions()).thenReturn(true);
            Mockito.when(oldMasterAgreementBean.isExoticNdo()).thenReturn(true);
            
            Mockito.when(oldMasterAgreementBean.isBullion()).thenReturn(false);
            Mockito.when(oldMasterAgreementBean.isBullionSpotFarward()).thenReturn(true);
            Mockito.when(oldMasterAgreementBean.isBullionOption()).thenReturn(true);
            Mockito.when(oldMasterAgreementBean.isBullionOptionExotic()).thenReturn(true);
            
            // Act
            com.traiana.bundle.fxweb.dna.screen.MasterAgreementBean.Tenor result = Whitebox.invokeMethod(target, "createOldMaTenor_null2MaxValue", oldMasterAgreementBean, oldMaTenor);
            
            // Assert
            org.junit.Assert.assertSame(clonedTenor, result);
            String maxVal = String.valueOf(Double.MAX_VALUE);
            
            Mockito.verify(clonedTenor).setCashSpotFwdMaxNumber(maxVal);
            Mockito.verify(clonedTenor).setNdfMaxNumber(maxVal);
            Mockito.verify(clonedTenor).setFxCashMaxNumber(maxVal);
            Mockito.verify(clonedTenor).setVanillaMaxNumber(maxVal);
            Mockito.verify(clonedTenor).setVanillaDeliverableMaxNumber(maxVal);
            Mockito.verify(clonedTenor).setNdoMaxNumber(maxVal);
            Mockito.verify(clonedTenor).setExoticOptionMaxNumber(maxVal);
            Mockito.verify(clonedTenor).setMultiLegOptionMaxNumber(maxVal);
            Mockito.verify(clonedTenor).setSingleBarrierMaxNumber(maxVal);
            Mockito.verify(clonedTenor).setDoubleBarrierMaxNumber(maxVal);
            Mockito.verify(clonedTenor).setDigitalMaxNumber(maxVal);
            Mockito.verify(clonedTenor).setBasketMaxNumber(maxVal);
            Mockito.verify(clonedTenor).setAsianOptionMaxNumber(maxVal);
            Mockito.verify(clonedTenor).setBermudanOptionMaxNumber(maxVal);
            Mockito.verify(clonedTenor).setExoticNdoMaxNumber(maxVal);
            Mockito.verify(clonedTenor).setFxOptionMaxNumber(maxVal);
            Mockito.verify(clonedTenor).setBullionSpotFwdMaxNumber(maxVal);
            Mockito.verify(clonedTenor).setBullionOptionsMaxNumber(maxVal);
            Mockito.verify(clonedTenor).setBullionOptionsExoticMaxNumber(maxVal);
            Mockito.verify(clonedTenor).setBullionMaxNumber(maxVal);
        }

        @Test
        public void test_createOldMaTenor_null2MaxValue_AllMainsTrue_NonNullLimits() throws Exception {
            // Arrange
            com.traiana.bundle.setup.dma.ma.MaSaveOp target = new com.traiana.bundle.setup.dma.ma.MaSaveOp();
            com.traiana.bundle.fxweb.dna.screen.MasterAgreementBean oldMasterAgreementBean = Mockito.mock(com.traiana.bundle.fxweb.dna.screen.MasterAgreementBean.class);
            com.traiana.bundle.fxweb.dna.screen.MasterAgreementBean.Tenor oldMaTenor = Mockito.mock(com.traiana.bundle.fxweb.dna.screen.MasterAgreementBean.Tenor.class);
            com.traiana.bundle.fxweb.dna.screen.MasterAgreementBean.Tenor clonedTenor = Mockito.mock(com.traiana.bundle.fxweb.dna.screen.MasterAgreementBean.Tenor.class);
            
            Mockito.when(oldMaTenor.clone()).thenReturn(clonedTenor);
            Mockito.when(oldMasterAgreementBean.isFxCash()).thenReturn(true);
            Mockito.when(oldMasterAgreementBean.isFxOptions()).thenReturn(true);
            Mockito.when(oldMasterAgreementBean.isBullion()).thenReturn(true);
            
            String val = "100.0";
            Mockito.when(clonedTenor.getFxCashMaxNumber()).thenReturn(val);
            Mockito.when(clonedTenor.getCashSpotFwdMaxNumber()).thenReturn(val);
            Mockito.when(clonedTenor.getNdfMaxNumber()).thenReturn(val);
            Mockito.when(clonedTenor.getFxOptionMaxNumber()).thenReturn(val);
            Mockito.when(clonedTenor.getVanillaMaxNumber()).thenReturn(val);
            Mockito.when(clonedTenor.getVanillaDeliverableMaxNumber()).thenReturn(val);
            Mockito.when(clonedTenor.getExoticOptionMaxNumber()).thenReturn(val);
            Mockito.when(clonedTenor.getNdoMaxNumber()).thenReturn(val);
            Mockito.when(clonedTenor.getMultiLegOptionMaxNumber()).thenReturn(val);
            Mockito.when(clonedTenor.getSingleBarrierMaxNumber()).thenReturn(val);
            Mockito.when(clonedTenor.getDoubleBarrierMaxNumber()).thenReturn(val);
            Mockito.when(clonedTenor.getDigitalMaxNumber()).thenReturn(val);
            Mockito.when(clonedTenor.getBasketMaxNumber()).thenReturn(val);
            Mockito.when(clonedTenor.getAsianOptionMaxNumber()).thenReturn(val);
            Mockito.when(clonedTenor.getBermudanOptionMaxNumber()).thenReturn(val);
            Mockito.when(clonedTenor.getExoticNdoMaxNumber()).thenReturn(val);
            Mockito.when(clonedTenor.getBullionMaxNumber()).thenReturn(val);
            Mockito.when(clonedTenor.getBullionSpotFwdMaxNumber()).thenReturn(val);
            Mockito.when(clonedTenor.getBullionOptionsMaxNumber()).thenReturn(val);
            Mockito.when(clonedTenor.getBullionOptionsExoticMaxNumber()).thenReturn(val);
            
            // Act
            com.traiana.bundle.fxweb.dna.screen.MasterAgreementBean.Tenor result = Whitebox.invokeMethod(target, "createOldMaTenor_null2MaxValue", oldMasterAgreementBean, oldMaTenor);
            
            // Assert
            org.junit.Assert.assertSame(clonedTenor, result);
            Mockito.verify(clonedTenor, Mockito.never()).setFxCashMaxNumber(Mockito.anyString());
            Mockito.verify(clonedTenor, Mockito.never()).setFxOptionMaxNumber(Mockito.anyString());
            Mockito.verify(clonedTenor, Mockito.never()).setBullionMaxNumber(Mockito.anyString());
            Mockito.verify(clonedTenor, Mockito.never()).setCashSpotFwdMaxNumber(Mockito.anyString());
            Mockito.verify(clonedTenor, Mockito.never()).setVanillaMaxNumber(Mockito.anyString());
            Mockito.verify(clonedTenor, Mockito.never()).setBullionSpotFwdMaxNumber(Mockito.anyString());
        }

        @Test
        public void test_createOldMaTenor_null2MaxValue_AllMainsFalse_SubsTrue_NonNullLimits() throws Exception {
            // Arrange
            com.traiana.bundle.setup.dma.ma.MaSaveOp target = new com.traiana.bundle.setup.dma.ma.MaSaveOp();
            com.traiana.bundle.fxweb.dna.screen.MasterAgreementBean oldMasterAgreementBean = Mockito.mock(com.traiana.bundle.fxweb.dna.screen.MasterAgreementBean.class);
            com.traiana.bundle.fxweb.dna.screen.MasterAgreementBean.Tenor oldMaTenor = Mockito.mock(com.traiana.bundle.fxweb.dna.screen.MasterAgreementBean.Tenor.class);
            com.traiana.bundle.fxweb.dna.screen.MasterAgreementBean.Tenor clonedTenor = Mockito.mock(com.traiana.bundle.fxweb.dna.screen.MasterAgreementBean.Tenor.class);
            
            Mockito.when(oldMaTenor.clone()).thenReturn(clonedTenor);
            Mockito.when(oldMasterAgreementBean.isFxCash()).thenReturn(false);
            Mockito.when(oldMasterAgreementBean.isFxOptions()).thenReturn(false);
            Mockito.when(oldMasterAgreementBean.isBullion()).thenReturn(false);
            
            Mockito.when(oldMasterAgreementBean.isSpotFarward()).thenReturn(true);
            Mockito.when(oldMasterAgreementBean.isNdf()).thenReturn(true);
            Mockito.when(oldMasterAgreementBean.isVanillaOptionsFamily()).thenReturn(true);
            Mockito.when(oldMasterAgreementBean.isBullionSpotFarward()).thenReturn(true);
            
            String val = "100.0";
            Mockito.when(clonedTenor.getCashSpotFwdMaxNumber()).thenReturn(val);
            Mockito.when(clonedTenor.getNdfMaxNumber()).thenReturn(val);
            Mockito.when(clonedTenor.getVanillaMaxNumber()).thenReturn(val);
            Mockito.when(clonedTenor.getBullionSpotFwdMaxNumber()).thenReturn(val);
            
            // Act
            com.traiana.bundle.fxweb.dna.screen.MasterAgreementBean.Tenor result = Whitebox.invokeMethod(target, "createOldMaTenor_null2MaxValue", oldMasterAgreementBean, oldMaTenor);
            
            // Assert
            org.junit.Assert.assertSame(clonedTenor, result);
            Mockito.verify(clonedTenor, Mockito.never()).setCashSpotFwdMaxNumber(Mockito.anyString());
            Mockito.verify(clonedTenor, Mockito.never()).setFxCashMaxNumber(Mockito.anyString());
            Mockito.verify(clonedTenor, Mockito.never()).setVanillaMaxNumber(Mockito.anyString());
            Mockito.verify(clonedTenor, Mockito.never()).setFxOptionMaxNumber(Mockito.anyString());
            Mockito.verify(clonedTenor, Mockito.never()).setBullionSpotFwdMaxNumber(Mockito.anyString());
            Mockito.verify(clonedTenor, Mockito.never()).setBullionMaxNumber(Mockito.anyString());
        }

        @Test
        public void test_createOldMaTenor_null2MaxValue_AllFalse_NullLimits() throws Exception {
            // Arrange
            com.traiana.bundle.setup.dma.ma.MaSaveOp target = new com.traiana.bundle.setup.dma.ma.MaSaveOp();
            com.traiana.bundle.fxweb.dna.screen.MasterAgreementBean oldMasterAgreementBean = Mockito.mock(com.traiana.bundle.fxweb.dna.screen.MasterAgreementBean.class);
            com.traiana.bundle.fxweb.dna.screen.MasterAgreementBean.Tenor oldMaTenor = Mockito.mock(com.traiana.bundle.fxweb.dna.screen.MasterAgreementBean.Tenor.class);
            com.traiana.bundle.fxweb.dna.screen.MasterAgreementBean.Tenor clonedTenor = Mockito.mock(com.traiana.bundle.fxweb.dna.screen.MasterAgreementBean.Tenor.class);
            
            Mockito.when(oldMaTenor.clone()).thenReturn(clonedTenor);
            
            // Act
            com.traiana.bundle.fxweb.dna.screen.MasterAgreementBean.Tenor result = Whitebox.invokeMethod(target, "createOldMaTenor_null2MaxValue", oldMasterAgreementBean, oldMaTenor);
            
            // Assert
            org.junit.Assert.assertSame(clonedTenor, result);
            Mockito.verify(clonedTenor, Mockito.never()).setFxCashMaxNumber(Mockito.anyString());
            Mockito.verify(clonedTenor, Mockito.never()).setCashSpotFwdMaxNumber(Mockito.anyString());
            Mockito.verify(clonedTenor, Mockito.never()).setNdfMaxNumber(Mockito.anyString());
            Mockito.verify(clonedTenor, Mockito.never()).setFxOptionMaxNumber(Mockito.anyString());
            Mockito.verify(clonedTenor, Mockito.never()).setVanillaMaxNumber(Mockito.anyString());
            Mockito.verify(clonedTenor, Mockito.never()).setBullionMaxNumber(Mockito.anyString());
        }

        @Test
        public void test_createOldMaTenor_null2MaxValue_SpotFarwardTrue() throws Exception {
            // Arrange
            com.traiana.bundle.setup.dma.ma.MaSaveOp target = new com.traiana.bundle.setup.dma.ma.MaSaveOp();
            com.traiana.bundle.fxweb.dna.screen.MasterAgreementBean oldMasterAgreementBean = Mockito.mock(com.traiana.bundle.fxweb.dna.screen.MasterAgreementBean.class);
            com.traiana.bundle.fxweb.dna.screen.MasterAgreementBean.Tenor oldMaTenor = Mockito.mock(com.traiana.bundle.fxweb.dna.screen.MasterAgreementBean.Tenor.class);
            com.traiana.bundle.fxweb.dna.screen.MasterAgreementBean.Tenor clonedTenor = Mockito.mock(com.traiana.bundle.fxweb.dna.screen.MasterAgreementBean.Tenor.class);
            
            Mockito.when(oldMaTenor.clone()).thenReturn(clonedTenor);
            Mockito.when(oldMasterAgreementBean.isFxCash()).thenReturn(false);
            Mockito.when(oldMasterAgreementBean.isSpotFarward()).thenReturn(true);
            Mockito.when(oldMasterAgreementBean.isFarward()).thenReturn(false);
            
            // Act
            com.traiana.bundle.fxweb.dna.screen.MasterAgreementBean.Tenor result = Whitebox.invokeMethod(target, "createOldMaTenor_null2MaxValue", oldMasterAgreementBean, oldMaTenor);
            
            // Assert
            org.junit.Assert.assertSame(clonedTenor, result);
            String maxVal = String.valueOf(Double.MAX_VALUE);
            Mockito.verify(clonedTenor).setCashSpotFwdMaxNumber(maxVal);
            Mockito.verify(clonedTenor).setFxCashMaxNumber(maxVal);
        }

        @Test
        public void test_validate_EBMissingReadOnly_ReturnsError() throws Exception {
            com.traiana.bundle.core.session.AppWorkspaceSession session = org.powermock.api.mockito.PowerMockito.mock(com.traiana.bundle.core.session.AppWorkspaceSession.class);
            com.traiana.bundle.core.component.cache.impl.OrgPropsBean orgProps = org.powermock.api.mockito.PowerMockito.mock(com.traiana.bundle.core.component.cache.impl.OrgPropsBean.class);
            org.powermock.api.mockito.PowerMockito.when(session.getCurrentUserParentOrgProps()).thenReturn(orgProps);
            org.powermock.api.mockito.PowerMockito.when(orgProps.getType()).thenReturn(com.traiana.bundle.core.codes.OrganizationTypeConstsInterface.K_EB);
    
            org.powermock.api.mockito.PowerMockito.mockStatic(com.traiana.bundle.core.util.OrganizationUtil.class);
            com.traiana.bundle.core.component.cache.impl.OrgPropsBean pbProps = org.powermock.api.mockito.PowerMockito.mock(com.traiana.bundle.core.component.cache.impl.OrgPropsBean.class);
            org.powermock.api.mockito.PowerMockito.when(com.traiana.bundle.core.util.OrganizationUtil.getOrgPropsIncludeDidabled(Mockito.anyString(), Mockito.any(com.traiana.platform.sys.RouteInfoInterface.class))).thenReturn(pbProps);
            org.powermock.api.mockito.PowerMockito.when(pbProps.getDnmEnableDType()).thenReturn("OTHER");
    
            com.traiana.bundle.fxweb.dna.screen.MasterAgreementBean maBean = org.powermock.api.mockito.PowerMockito.mock(com.traiana.bundle.fxweb.dna.screen.MasterAgreementBean.class);
            com.traiana.platform.sys.RouteInfoInterface routeInfo = org.powermock.api.mockito.PowerMockito.mock(com.traiana.platform.sys.RouteInfoInterface.class);
    
            com.traiana.bundle.setup.dma.ma.MaSaveOp target = org.powermock.api.mockito.PowerMockito.spy(org.powermock.reflect.Whitebox.newInstance(com.traiana.bundle.setup.dma.ma.MaSaveOp.class));
            org.powermock.reflect.Whitebox.setInternalState(target, "session", session);
            org.powermock.reflect.Whitebox.setInternalState(target, "maBean", maBean);
            org.powermock.reflect.Whitebox.setInternalState(target, "routeInfo", routeInfo);
    
            long result = (long) org.powermock.reflect.Whitebox.invokeMethod(target, "validate");
            org.junit.Assert.assertEquals(com.traiana.bundle.core.FsLogFinals.MA_CANNT_SAVE_BY_EB, result);
        }

        @Test
        public void test_validate_EBCatchException_ReturnsError() throws Exception {
            com.traiana.bundle.core.session.AppWorkspaceSession session = org.powermock.api.mockito.PowerMockito.mock(com.traiana.bundle.core.session.AppWorkspaceSession.class);
            com.traiana.bundle.core.component.cache.impl.OrgPropsBean orgProps = org.powermock.api.mockito.PowerMockito.mock(com.traiana.bundle.core.component.cache.impl.OrgPropsBean.class);
            org.powermock.api.mockito.PowerMockito.when(session.getCurrentUserParentOrgProps()).thenReturn(orgProps);
            org.powermock.api.mockito.PowerMockito.when(orgProps.getType()).thenReturn(com.traiana.bundle.core.codes.OrganizationTypeConstsInterface.K_EB);
    
            org.powermock.api.mockito.PowerMockito.mockStatic(com.traiana.bundle.core.util.OrganizationUtil.class);
            org.powermock.api.mockito.PowerMockito.when(com.traiana.bundle.core.util.OrganizationUtil.getOrgPropsIncludeDidabled(Mockito.anyString(), Mockito.any(com.traiana.platform.sys.RouteInfoInterface.class))).thenThrow(new RuntimeException("Simulated exception"));
    
            com.traiana.bundle.fxweb.dna.screen.MasterAgreementBean maBean = org.powermock.api.mockito.PowerMockito.mock(com.traiana.bundle.fxweb.dna.screen.MasterAgreementBean.class);
            com.traiana.platform.sys.RouteInfoInterface routeInfo = org.powermock.api.mockito.PowerMockito.mock(com.traiana.platform.sys.RouteInfoInterface.class);
    
            com.traiana.bundle.setup.dma.ma.MaSaveOp target = org.powermock.api.mockito.PowerMockito.spy(org.powermock.reflect.Whitebox.newInstance(com.traiana.bundle.setup.dma.ma.MaSaveOp.class));
            org.powermock.reflect.Whitebox.setInternalState(target, "session", session);
            org.powermock.reflect.Whitebox.setInternalState(target, "maBean", maBean);
            org.powermock.reflect.Whitebox.setInternalState(target, "routeInfo", routeInfo);
    
            long result = (long) org.powermock.reflect.Whitebox.invokeMethod(target, "validate");
            org.junit.Assert.assertEquals(com.traiana.bundle.core.FsLogFinals.MA_CANNT_SAVE_BY_EB, result);
        }

        @Test
        public void test_validate_EmptyLimits_ReturnsError() throws Exception {
            com.traiana.bundle.core.session.AppWorkspaceSession session = org.powermock.api.mockito.PowerMockito.mock(com.traiana.bundle.core.session.AppWorkspaceSession.class);
            com.traiana.bundle.core.component.cache.impl.OrgPropsBean orgProps = org.powermock.api.mockito.PowerMockito.mock(com.traiana.bundle.core.component.cache.impl.OrgPropsBean.class);
            org.powermock.api.mockito.PowerMockito.when(session.getCurrentUserParentOrgProps()).thenReturn(orgProps);
            org.powermock.api.mockito.PowerMockito.when(orgProps.getType()).thenReturn(0);
    
            com.traiana.bundle.fxweb.dna.screen.MasterAgreementBean maBean = org.powermock.api.mockito.PowerMockito.mock(com.traiana.bundle.fxweb.dna.screen.MasterAgreementBean.class);
            org.powermock.api.mockito.PowerMockito.when(maBean.getLimitsList()).thenReturn(new java.util.ArrayList());
    
            com.traiana.bundle.setup.dma.ma.MaSaveOp target = org.powermock.api.mockito.PowerMockito.spy(org.powermock.reflect.Whitebox.newInstance(com.traiana.bundle.setup.dma.ma.MaSaveOp.class));
            org.powermock.reflect.Whitebox.setInternalState(target, "session", session);
            org.powermock.reflect.Whitebox.setInternalState(target, "maBean", maBean);
    
            org.powermock.api.mockito.PowerMockito.doNothing().when(target, "validateLegalEntity", Mockito.anyString(), Mockito.anyList());
    
            long result = (long) org.powermock.reflect.Whitebox.invokeMethod(target, "validate");
            org.junit.Assert.assertEquals(com.traiana.bundle.core.FsLogFinals.SELECT_AT_LEAST_ONE_LIMIT, result);
        }

        @Test
        public void test_insertMaseterAgreement_activeStatus_fullData() throws Exception {
            // Arrange
            com.traiana.bundle.setup.dma.ma.MaSaveOp target = new com.traiana.bundle.setup.dma.ma.MaSaveOp();
            com.traiana.bundle.setup.dma.ma.MaSaveOp spy = org.powermock.api.mockito.PowerMockito.spy(target);
    
            int maStatus = com.traiana.bundle.core.codes.MasterAgreementStatusInterface.K_ACTIVE;
    
            org.powermock.api.mockito.PowerMockito.doNothing().when(spy, "updatePendingEdnsToSuspended", Mockito.anyString());
            org.powermock.api.mockito.PowerMockito.doReturn(100L).when(spy, "convertLegalEntityName2ID", Mockito.anyList(), Mockito.anyString());
    
            com.traiana.bundle.fxweb.dna.screen.MasterAgreementBean maBean = org.mockito.Mockito.mock(com.traiana.bundle.fxweb.dna.screen.MasterAgreementBean.class);
            org.mockito.Mockito.when(maBean.getId()).thenReturn("ID_123");
            org.mockito.Mockito.when(maBean.getEb()).thenReturn("EB_VAL");
            org.mockito.Mockito.when(maBean.getPb()).thenReturn("PB_VAL");
            org.mockito.Mockito.when(maBean.getIsda()).thenReturn(new java.util.Date());
            org.mockito.Mockito.when(maBean.getMasterGiveup()).thenReturn(new java.util.Date());
            org.mockito.Mockito.when(maBean.getEmta()).thenReturn(new java.util.Date());
            org.mockito.Mockito.when(maBean.getLegalEntitiestForEBList()).thenReturn(new java.util.ArrayList());
            org.mockito.Mockito.when(maBean.getEbLegalEntity()).thenReturn("EB_LE");
            org.mockito.Mockito.when(maBean.getLegalEntitiestForPBList()).thenReturn(new java.util.ArrayList());
            org.mockito.Mockito.when(maBean.getPbLegalEntity()).thenReturn("PB_LE");
            
            org.mockito.Mockito.when(maBean.isApprovedInPass()).thenReturn(true);
            org.mockito.Mockito.when(maBean.isLastVersion()).thenReturn(true);
            org.mockito.Mockito.when(maBean.isEbCanView()).thenReturn(true);
            org.mockito.Mockito.when(maBean.getSpotRate()).thenReturn(true);
            org.mockito.Mockito.when(maBean.isFxCash()).thenReturn(true);
            org.mockito.Mockito.when(maBean.isSpotFarward()).thenReturn(true);
            org.mockito.Mockito.when(maBean.isFarward()).thenReturn(true);
            org.mockito.Mockito.when(maBean.isNdf()).thenReturn(true);
            org.mockito.Mockito.when(maBean.isFxOptions()).thenReturn(true);
            org.mockito.Mockito.when(maBean.isVanillaOptions()).thenReturn(true);
            org.mockito.Mockito.when(maBean.isEuropeanOptions()).thenReturn(true);
            org.mockito.Mockito.when(maBean.isAmericanOptions()).thenReturn(true);
            org.mockito.Mockito.when(maBean.isBermudanOptions()).thenReturn(true);
            org.mockito.Mockito.when(maBean.isAsianOptions()).thenReturn(true);
            org.mockito.Mockito.when(maBean.isExoticOption()).thenReturn(true);
            org.mockito.Mockito.when(maBean.isNdoOptions()).thenReturn(true);
            org.mockito.Mockito.when(maBean.isMultiLegOptions()).thenReturn(true);
            org.mockito.Mockito.when(maBean.isSingleBarrierOptions()).thenReturn(true);
            org.mockito.Mockito.when(maBean.isDoubleBarrierOptions()).thenReturn(true);
            org.mockito.Mockito.when(maBean.isDigitalOptions()).thenReturn(true);
            org.mockito.Mockito.when(maBean.isEuropeanDigital()).thenReturn(true);
            org.mockito.Mockito.when(maBean.isEuropeanDigitalWithKnockIn()).thenReturn(true);
            org.mockito.Mockito.when(maBean.isEuropeanDigitalWithKnockOut()).thenReturn(true);
            org.mockito.Mockito.when(maBean.isOneTouchPayoutAtMaturity()).thenReturn(true);
            org.mockito.Mockito.when(maBean.isOneTouchInstantPayout()).thenReturn(true);
            org.mockito.Mockito.when(maBean.isNoTouch()).thenReturn(true);
            org.mockito.Mockito.when(maBean.isDoubleOneTouch()).thenReturn(true);
            org.mockito.Mockito.when(maBean.isDoubleNoTouch()).thenReturn(true);
            org.mockito.Mockito.when(maBean.isWindowSingleBarrierOptions()).thenReturn(true);
            org.mockito.Mockito.when(maBean.isWindowDoubleBarrierOptions()).thenReturn(true);
            org.mockito.Mockito.when(maBean.isBasketOptions()).thenReturn(true);
            org.mockito.Mockito.when(maBean.isExoticNdo()).thenReturn(true);
            org.mockito.Mockito.when(maBean.isBullion()).thenReturn(true);
            org.mockito.Mockito.when(maBean.isBullionSpotFarward()).thenReturn(true);
            org.mockito.Mockito.when(maBean.isBullionOption()).thenReturn(true);
            org.mockito.Mockito.when(maBean.isBullionOptionExotic()).thenReturn(true);
            org.mockito.Mockito.when(maBean.isEbApprovalRequired()).thenReturn(true);
            org.mockito.Mockito.when(maBean.isAllowSettingTenorPLevel()).thenReturn(true);
            org.mockito.Mockito.when(maBean.isMainBullion()).thenReturn(true);
    
            org.powermock.reflect.Whitebox.setInternalState(spy, "maBean", maBean);
    
            com.traiana.bundle.core.session.AppWorkspaceSession sessionImpl = org.mockito.Mockito.mock(com.traiana.bundle.core.session.AppWorkspaceSession.class);
            org.mockito.Mockito.when(sessionImpl.getCurrentUserId()).thenReturn(777L);
            org.powermock.reflect.Whitebox.setInternalState(spy, "sessionImpl", sessionImpl);
    
            Object routeInfo = org.mockito.Mockito.mock(com.traiana.platform.sys.RouteInfoInterface.class);
            org.powermock.reflect.Whitebox.setInternalState(spy, "routeInfo", routeInfo);
    
            org.powermock.api.mockito.PowerMockito.mockStatic(com.traiana.bundle.core.util.OrganizationUtil.class);
            com.traiana.bundle.core.component.cache.impl.OrgPropsBean orgProps = org.mockito.Mockito.mock(com.traiana.bundle.core.component.cache.impl.OrgPropsBean.class);
            org.mockito.Mockito.when(orgProps.getId()).thenReturn(10L);
            org.mockito.Mockito.when(orgProps.getDnmEnableDType()).thenReturn("TypeA");
            org.powermock.api.mockito.PowerMockito.when(com.traiana.bundle.core.util.OrganizationUtil.getOrgPropsIncludeDidabled(Mockito.anyString(), Mockito.any(com.traiana.platform.sys.RouteInfoInterface.class))).thenReturn(orgProps);
    
            org.powermock.api.mockito.PowerMockito.mockStatic(com.traiana.workspace.bean.BeansWorkspace.class);
            com.traiana.workspace.bean.BeansWorkspaceInterface workspaceMock = org.mockito.Mockito.mock(com.traiana.workspace.bean.BeansWorkspaceInterface.class);
            org.powermock.api.mockito.PowerMockito.when(com.traiana.workspace.bean.BeansWorkspace.getWorkspace()).thenReturn(workspaceMock);
    
            org.mockito.Mockito.doAnswer(new org.mockito.stubbing.Answer<Void>() {
                @Override
                public Void answer(org.mockito.invocation.InvocationOnMock invocation) throws Throwable {
                    java.util.Map<String, Object> params = invocation.getArgument(3);
                    params.put(com.traiana.workspace.bean.ServiceFinals.ID, 999L);
                    return null;
                }
            }).when(workspaceMock).doService(Mockito.anyString(), Mockito.anyString(), Mockito.any(), Mockito.anyMap(), Mockito.any(), Mockito.anyString());
    
            org.powermock.reflect.Whitebox.setInternalState(spy, "connection", Mockito.mock(Connection.class));
            org.powermock.reflect.Whitebox.setInternalState(spy, "session", org.mockito.Mockito.mock(com.traiana.workspace.session.WorkspaceSession.class));
            org.powermock.reflect.Whitebox.setInternalState(spy, "userName", "testUser");
    
            // Act
            Long result = org.powermock.reflect.Whitebox.invokeMethod(spy, "insertMaseterAgreement", maStatus);
    
            // Assert
            org.junit.Assert.assertEquals(Long.valueOf(999L), result);
            org.mockito.Mockito.verify(maBean).setRejectReason(null);
            org.mockito.Mockito.verify(maBean).setUser(777L);
            org.mockito.Mockito.verify(maBean).setEbCanView(false);
            org.mockito.Mockito.verify(maBean).setLastVersion(false);
            org.mockito.Mockito.verify(maBean).setApprovalID(null);
            org.mockito.Mockito.verify(maBean).setDnmEnabledType("TypeA");
        }

        @Test
        public void test_insertMaseterAgreement_inactiveStatus_emptyData() throws Exception {
            // Arrange
            com.traiana.bundle.setup.dma.ma.MaSaveOp target = new com.traiana.bundle.setup.dma.ma.MaSaveOp();
            com.traiana.bundle.setup.dma.ma.MaSaveOp spy = org.powermock.api.mockito.PowerMockito.spy(target);
    
            int maStatus = com.traiana.bundle.core.codes.MasterAgreementStatusInterface.K_ACTIVE + 1;
    
            org.powermock.api.mockito.PowerMockito.doReturn(100L).when(spy, "convertLegalEntityName2ID", Mockito.anyList(), Mockito.anyString());
    
            com.traiana.bundle.fxweb.dna.screen.MasterAgreementBean maBean = org.mockito.Mockito.mock(com.traiana.bundle.fxweb.dna.screen.MasterAgreementBean.class);
            
            String emptyStr = com.traiana.workspace.WorkspaceFinals.EMPTY_STR;
            org.mockito.Mockito.when(maBean.getEb()).thenReturn(emptyStr);
            org.mockito.Mockito.when(maBean.getPb()).thenReturn(emptyStr);
            
            org.mockito.Mockito.when(maBean.getIsda()).thenReturn(null);
            org.mockito.Mockito.when(maBean.getMasterGiveup()).thenReturn(null);
            org.mockito.Mockito.when(maBean.getEmta()).thenReturn(null);
            org.mockito.Mockito.when(maBean.getLegalEntitiestForEBList()).thenReturn(new java.util.ArrayList());
            org.mockito.Mockito.when(maBean.getEbLegalEntity()).thenReturn(null);
            org.mockito.Mockito.when(maBean.getLegalEntitiestForPBList()).thenReturn(new java.util.ArrayList());
            org.mockito.Mockito.when(maBean.getPbLegalEntity()).thenReturn(null);
    
            org.mockito.Mockito.when(maBean.isApprovedInPass()).thenReturn(false);
            org.mockito.Mockito.when(maBean.isLastVersion()).thenReturn(false);
            org.mockito.Mockito.when(maBean.isEbCanView()).thenReturn(false);
            org.mockito.Mockito.when(maBean.getSpotRate()).thenReturn(false);
            org.mockito.Mockito.when(maBean.isFxCash()).thenReturn(false);
            org.mockito.Mockito.when(maBean.isSpotFarward()).thenReturn(false);
            org.mockito.Mockito.when(maBean.isFarward()).thenReturn(false);
            org.mockito.Mockito.when(maBean.isNdf()).thenReturn(false);
            org.mockito.Mockito.when(maBean.isFxOptions()).thenReturn(false);
            org.mockito.Mockito.when(maBean.isVanillaOptions()).thenReturn(false);
            org.mockito.Mockito.when(maBean.isEuropeanOptions()).thenReturn(false);
            org.mockito.Mockito.when(maBean.isAmericanOptions()).thenReturn(false);
            org.mockito.Mockito.when(maBean.isBermudanOptions()).thenReturn(false);
            org.mockito.Mockito.when(maBean.isAsianOptions()).thenReturn(false);
            org.mockito.Mockito.when(maBean.isExoticOption()).thenReturn(false);
            org.mockito.Mockito.when(maBean.isNdoOptions()).thenReturn(false);
            org.mockito.Mockito.when(maBean.isMultiLegOptions()).thenReturn(false);
            org.mockito.Mockito.when(maBean.isSingleBarrierOptions()).thenReturn(false);
            org.mockito.Mockito.when(maBean.isDoubleBarrierOptions()).thenReturn(false);
            org.mockito.Mockito.when(maBean.isDigitalOptions()).thenReturn(false);
            org.mockito.Mockito.when(maBean.isEuropeanDigital()).thenReturn(false);
            org.mockito.Mockito.when(maBean.isEuropeanDigitalWithKnockIn()).thenReturn(false);
            org.mockito.Mockito.when(maBean.isEuropeanDigitalWithKnockOut()).thenReturn(false);
            org.mockito.Mockito.when(maBean.isOneTouchPayoutAtMaturity()).thenReturn(false);
            org.mockito.Mockito.when(maBean.isOneTouchInstantPayout()).thenReturn(false);
            org.mockito.Mockito.when(maBean.isNoTouch()).thenReturn(false);
            org.mockito.Mockito.when(maBean.isDoubleOneTouch()).thenReturn(false);
            org.mockito.Mockito.when(maBean.isDoubleNoTouch()).thenReturn(false);
            org.mockito.Mockito.when(maBean.isWindowSingleBarrierOptions()).thenReturn(false);
            org.mockito.Mockito.when(maBean.isWindowDoubleBarrierOptions()).thenReturn(false);
            org.mockito.Mockito.when(maBean.isBasketOptions()).thenReturn(false);
            org.mockito.Mockito.when(maBean.isExoticNdo()).thenReturn(false);
            org.mockito.Mockito.when(maBean.isBullion()).thenReturn(false);
            org.mockito.Mockito.when(maBean.isBullionSpotFarward()).thenReturn(false);
            org.mockito.Mockito.when(maBean.isBullionOption()).thenReturn(false);
            org.mockito.Mockito.when(maBean.isBullionOptionExotic()).thenReturn(false);
            org.mockito.Mockito.when(maBean.isEbApprovalRequired()).thenReturn(false);
            org.mockito.Mockito.when(maBean.isAllowSettingTenorPLevel()).thenReturn(false);
            org.mockito.Mockito.when(maBean.isMainBullion()).thenReturn(false);
    
            org.powermock.reflect.Whitebox.setInternalState(spy, "maBean", maBean);
    
            com.traiana.bundle.core.session.AppWorkspaceSession sessionImpl = org.mockito.Mockito.mock(com.traiana.bundle.core.session.AppWorkspaceSession.class);
            org.mockito.Mockito.when(sessionImpl.getCurrentUserId()).thenReturn(777L);
            org.powermock.reflect.Whitebox.setInternalState(spy, "sessionImpl", sessionImpl);
    
            org.powermock.reflect.Whitebox.setInternalState(spy, "routeInfo", org.mockito.Mockito.mock(com.traiana.platform.sys.RouteInfoInterface.class));
    
            org.powermock.api.mockito.PowerMockito.mockStatic(com.traiana.bundle.core.util.OrganizationUtil.class);
    
            org.powermock.api.mockito.PowerMockito.mockStatic(com.traiana.workspace.bean.BeansWorkspace.class);
            com.traiana.workspace.bean.BeansWorkspaceInterface workspaceMock = org.mockito.Mockito.mock(com.traiana.workspace.bean.BeansWorkspaceInterface.class);
            org.powermock.api.mockito.PowerMockito.when(com.traiana.workspace.bean.BeansWorkspace.getWorkspace()).thenReturn(workspaceMock);
    
            org.mockito.Mockito.doAnswer(new org.mockito.stubbing.Answer<Void>() {
                @Override
                public Void answer(org.mockito.invocation.InvocationOnMock invocation) throws Throwable {
                    java.util.Map<String, Object> params = invocation.getArgument(3);
                    params.put(com.traiana.workspace.bean.ServiceFinals.ID, 888L);
                    return null;
                }
            }).when(workspaceMock).doService(Mockito.anyString(), Mockito.anyString(), Mockito.any(), Mockito.anyMap(), Mockito.any(), Mockito.anyString());
    
            org.powermock.reflect.Whitebox.setInternalState(spy, "connection", Mockito.mock(Connection.class));
            org.powermock.reflect.Whitebox.setInternalState(spy, "session", org.mockito.Mockito.mock(com.traiana.workspace.session.WorkspaceSession.class));
            org.powermock.reflect.Whitebox.setInternalState(spy, "userName", "testUser");
    
            // Act
            Long result = org.powermock.reflect.Whitebox.invokeMethod(spy, "insertMaseterAgreement", maStatus);
    
            // Assert
            org.junit.Assert.assertEquals(Long.valueOf(888L), result);
            org.powermock.api.mockito.PowerMockito.verifyStatic(com.traiana.bundle.core.util.OrganizationUtil.class, org.mockito.Mockito.never());
            com.traiana.bundle.core.util.OrganizationUtil.getOrgPropsIncludeDidabled(Mockito.anyString(), Mockito.any(com.traiana.platform.sys.RouteInfoInterface.class));
        }

        @org.junit.Test
        @org.powermock.core.classloader.annotations.PrepareForTest(com.traiana.bundle.setup.dma.ma.MaSaveOp.class)
        public void test_work_modifyStateNarrowWinOkPressed_expandChanges_returnsEarly() throws Exception {
            com.traiana.bundle.setup.dma.ma.MaSaveOp target = org.powermock.api.mockito.PowerMockito.mock(com.traiana.bundle.setup.dma.ma.MaSaveOp.class, org.mockito.Mockito.CALLS_REAL_METHODS);
            com.traiana.bundle.fxweb.dna.screen.MasterAgreementBean maBean = org.mockito.Mockito.mock(com.traiana.bundle.fxweb.dna.screen.MasterAgreementBean.class);
            com.traiana.bundle.fxweb.dna.screen.ExpandChanges expandChanges = org.mockito.Mockito.mock(com.traiana.bundle.fxweb.dna.screen.ExpandChanges.class);
            org.mockito.Mockito.when(maBean.getExpandChanges()).thenReturn(expandChanges);
            org.powermock.reflect.Whitebox.setInternalState(target, "maBean", maBean);
    
            org.powermock.api.mockito.PowerMockito.doNothing().when(target, "checkIsPendingEbAcceptance");
    
            org.mockito.Mockito.when(maBean.getModifyProcessState()).thenReturn(com.traiana.bundle.setup.dma.DnmFinals.MA_MODIFY_STATE.NARROW_WIN__OK_PRESSED);
            org.mockito.Mockito.when(maBean.isPbAcceptance_()).thenReturn(false);
            org.mockito.Mockito.when(expandChanges.isExpandChanges()).thenReturn(true);
    
            org.powermock.reflect.Whitebox.invokeMethod(target, "work");
    
            org.mockito.Mockito.verify(maBean).setModifyProcessState(com.traiana.bundle.setup.dma.DnmFinals.MA_MODIFY_STATE.OPEN_EXPAND_WIN);
        }

        @org.junit.Test
        @org.powermock.core.classloader.annotations.PrepareForTest(com.traiana.bundle.setup.dma.ma.MaSaveOp.class)
        public void test_work_modifyStateNarrowWinOkPressed_noExpandChanges_saveProcessSucceed() throws Exception {
            com.traiana.bundle.setup.dma.ma.MaSaveOp target = org.powermock.api.mockito.PowerMockito.mock(com.traiana.bundle.setup.dma.ma.MaSaveOp.class, org.mockito.Mockito.CALLS_REAL_METHODS);
            com.traiana.bundle.fxweb.dna.screen.MasterAgreementBean maBean = org.mockito.Mockito.mock(com.traiana.bundle.fxweb.dna.screen.MasterAgreementBean.class);
            com.traiana.bundle.fxweb.dna.screen.ExpandChanges expandChanges = org.mockito.Mockito.mock(com.traiana.bundle.fxweb.dna.screen.ExpandChanges.class);
            com.traiana.workspace.session.WorkspaceSession session = org.mockito.Mockito.mock(com.traiana.workspace.session.WorkspaceSession.class);
            org.mockito.Mockito.when(maBean.getExpandChanges()).thenReturn(expandChanges);
            org.powermock.reflect.Whitebox.setInternalState(target, "maBean", maBean);
            org.powermock.reflect.Whitebox.setInternalState(target, "session", session);
            org.powermock.reflect.Whitebox.setInternalState(target, "action", "MODIFY");
            org.powermock.reflect.Whitebox.setInternalState(target, "saveProcessSucceed", true);
    
            org.powermock.api.mockito.PowerMockito.doNothing().when(target, "checkIsPendingEbAcceptance");
            org.powermock.api.mockito.PowerMockito.doNothing().when(target, "saveTheChangesInTheMaAnditsTheDnas");
            org.powermock.api.mockito.PowerMockito.doNothing().when(target, "approveMA");
            org.powermock.api.mockito.PowerMockito.doNothing().when(target, "prepareMessage");
    
            org.mockito.Mockito.when(maBean.getModifyProcessState()).thenReturn(com.traiana.bundle.setup.dma.DnmFinals.MA_MODIFY_STATE.NARROW_WIN__OK_PRESSED);
            org.mockito.Mockito.when(maBean.isPbAcceptance_()).thenReturn(false);
            org.mockito.Mockito.when(expandChanges.isExpandChanges()).thenReturn(false);
    
            org.powermock.reflect.Whitebox.invokeMethod(target, "work");
    
            org.mockito.Mockito.verify(maBean).setModifyProcessState(com.traiana.bundle.setup.dma.DnmFinals.MA_MODIFY_STATE.SAVE_ALL_CHANGES_IN_DB);
            org.powermock.api.mockito.PowerMockito.verifyPrivate(target).invoke("saveTheChangesInTheMaAnditsTheDnas");
            org.powermock.api.mockito.PowerMockito.verifyPrivate(target).invoke("approveMA");
            org.mockito.Mockito.verify(session).setAttribute(com.traiana.bundle.setup.dma.DnmFinals.NEW_FLAG, "false");
        }

        @org.junit.Test
        @org.powermock.core.classloader.annotations.PrepareForTest(com.traiana.bundle.setup.dma.ma.MaSaveOp.class)
        public void test_work_modifyStateNarrowWinOkPressed_pbAcceptance_saveProcessSucceed_inactive() throws Exception {
            com.traiana.bundle.setup.dma.ma.MaSaveOp target = org.powermock.api.mockito.PowerMockito.mock(com.traiana.bundle.setup.dma.ma.MaSaveOp.class, org.mockito.Mockito.CALLS_REAL_METHODS);
            com.traiana.bundle.fxweb.dna.screen.MasterAgreementBean maBean = org.mockito.Mockito.mock(com.traiana.bundle.fxweb.dna.screen.MasterAgreementBean.class);
            com.traiana.bundle.core.session.AppWorkspaceSession sessionImpl = org.mockito.Mockito.mock(com.traiana.bundle.core.session.AppWorkspaceSession.class);
            org.powermock.reflect.Whitebox.setInternalState(target, "maBean", maBean);
            org.powermock.reflect.Whitebox.setInternalState(target, "sessionImpl", sessionImpl);
            org.powermock.reflect.Whitebox.setInternalState(target, "saveProcessSucceed", true);
    
            org.powermock.api.mockito.PowerMockito.doNothing().when(target, "checkIsPendingEbAcceptance");
            org.powermock.api.mockito.PowerMockito.doNothing().when(target, "saveActiveMa_PendingForApproval");
            org.powermock.api.mockito.PowerMockito.doNothing().when(target, "clearApprovalList");
            org.powermock.api.mockito.PowerMockito.doNothing().when(target, "prepareMessage");
            org.powermock.api.mockito.PowerMockito.doNothing().when(target, "updateAppovalMaStatus");
    
            org.mockito.Mockito.when(maBean.getModifyProcessState()).thenReturn(com.traiana.bundle.setup.dma.DnmFinals.MA_MODIFY_STATE.NARROW_WIN__OK_PRESSED);
            org.mockito.Mockito.when(maBean.isPbAcceptance_()).thenReturn(true);
            org.mockito.Mockito.when(maBean.isApprovedInPass()).thenReturn(true);
            org.mockito.Mockito.when(maBean.getStatus()).thenReturn(String.valueOf(com.traiana.bundle.core.codes.MasterAgreementStatusInterface.K_INACTIVE));
    
            org.powermock.reflect.Whitebox.invokeMethod(target, "work");
    
            org.powermock.api.mockito.PowerMockito.verifyPrivate(target).invoke("saveActiveMa_PendingForApproval");
            org.mockito.Mockito.verify(maBean).setApprovalStatusAndNextApprovingEntity(String.valueOf(com.traiana.bundle.core.codes.MaApprovalStatusConstsInterface.K_PENDING_RE_APPROVE), com.traiana.bundle.setup.dma.DnmFinals.NEXT_APPRVING_ENTITY.PB);
            org.mockito.Mockito.verify(sessionImpl).setAttribute(com.traiana.bundle.setup.dma.DnmFinals.MA_STATUS, com.traiana.bundle.core.codes.MasterAgreementStatusInterface.V_INACTIVE);
        }

        @org.junit.Test
        @org.powermock.core.classloader.annotations.PrepareForTest(com.traiana.bundle.setup.dma.ma.MaSaveOp.class)
        public void test_work_modifyStateExpandWinValidation_returnsEarly() throws Exception {
            com.traiana.bundle.setup.dma.ma.MaSaveOp target = org.powermock.api.mockito.PowerMockito.mock(com.traiana.bundle.setup.dma.ma.MaSaveOp.class, org.mockito.Mockito.CALLS_REAL_METHODS);
            com.traiana.bundle.fxweb.dna.screen.MasterAgreementBean maBean = org.mockito.Mockito.mock(com.traiana.bundle.fxweb.dna.screen.MasterAgreementBean.class);
            org.powermock.reflect.Whitebox.setInternalState(target, "maBean", maBean);
    
            org.powermock.api.mockito.PowerMockito.doNothing().when(target, "checkIsPendingEbAcceptance");
            org.powermock.api.mockito.PowerMockito.doNothing().when(target, "expandValidation");
    
            org.mockito.Mockito.when(maBean.getModifyProcessState()).thenReturn(com.traiana.bundle.setup.dma.DnmFinals.MA_MODIFY_STATE.EXPAND_WIN__VALIDATION);
    
            org.powermock.reflect.Whitebox.invokeMethod(target, "work");
    
            org.powermock.api.mockito.PowerMockito.verifyPrivate(target).invoke("expandValidation");
        }

        @org.junit.Test
        @org.powermock.core.classloader.annotations.PrepareForTest(com.traiana.bundle.setup.dma.ma.MaSaveOp.class)
        public void test_work_modifyStateSaveAllChanges_noSuccess_validateMinusOne_actionSave_hasPermission() throws Exception {
            com.traiana.bundle.setup.dma.ma.MaSaveOp target = org.powermock.api.mockito.PowerMockito.mock(com.traiana.bundle.setup.dma.ma.MaSaveOp.class, org.mockito.Mockito.CALLS_REAL_METHODS);
            com.traiana.bundle.fxweb.dna.screen.MasterAgreementBean maBean = org.mockito.Mockito.mock(com.traiana.bundle.fxweb.dna.screen.MasterAgreementBean.class);
            com.traiana.workspace.session.WorkspaceSession session = org.mockito.Mockito.mock(com.traiana.workspace.session.WorkspaceSession.class);
            org.powermock.reflect.Whitebox.setInternalState(target, "maBean", maBean);
            org.powermock.reflect.Whitebox.setInternalState(target, "session", session);
            org.powermock.reflect.Whitebox.setInternalState(target, "saveProcessSucceed", false);
            org.powermock.reflect.Whitebox.setInternalState(target, "action", com.traiana.bundle.setup.dma.DnmFinals.DNM_ACTION_SAVE);
    
            org.powermock.api.mockito.PowerMockito.doNothing().when(target, "checkIsPendingEbAcceptance");
            org.powermock.api.mockito.PowerMockito.doNothing().when(target, "saveTheChangesInTheMaAnditsTheDnas");
            org.powermock.api.mockito.PowerMockito.doReturn(-1L).when(target, "validate");
            org.powermock.api.mockito.PowerMockito.doReturn(true).when(target, "hasPermission", Mockito.anyString(), Mockito.anyBoolean());
            org.powermock.api.mockito.PowerMockito.doNothing().when(target, "save", Mockito.anyInt());
    
            org.mockito.Mockito.when(maBean.getModifyProcessState()).thenReturn(com.traiana.bundle.setup.dma.DnmFinals.MA_MODIFY_STATE.SAVE_ALL_CHANGES_IN_DB);
    
            org.powermock.reflect.Whitebox.invokeMethod(target, "work");
    
            org.powermock.api.mockito.PowerMockito.verifyPrivate(target).invoke("saveTheChangesInTheMaAnditsTheDnas");
            org.mockito.Mockito.verify(session).setAttribute(com.traiana.bundle.setup.dma.DnmFinals.NEW_FLAG, "true");
            org.mockito.Mockito.verify(maBean).setEbApprovalRequired(true);
            org.powermock.api.mockito.PowerMockito.verifyPrivate(target).invoke("save", com.traiana.bundle.core.codes.MasterAgreementStatusInterface.K_INACTIVE);
        }

        @org.junit.Test
        @org.powermock.core.classloader.annotations.PrepareForTest(com.traiana.bundle.setup.dma.ma.MaSaveOp.class)
        public void test_work_modifyStateSaveAllChangesNoDna_noSuccess_validateMinusOne_actionModify_hasPermission_statusInactive() throws Exception {
            com.traiana.bundle.setup.dma.ma.MaSaveOp target = org.powermock.api.mockito.PowerMockito.mock(com.traiana.bundle.setup.dma.ma.MaSaveOp.class, org.mockito.Mockito.CALLS_REAL_METHODS);
            com.traiana.bundle.fxweb.dna.screen.MasterAgreementBean maBean = org.mockito.Mockito.mock(com.traiana.bundle.fxweb.dna.screen.MasterAgreementBean.class);
            com.traiana.workspace.session.WorkspaceSession session = org.mockito.Mockito.mock(com.traiana.workspace.session.WorkspaceSession.class);
            org.powermock.reflect.Whitebox.setInternalState(target, "maBean", maBean);
            org.powermock.reflect.Whitebox.setInternalState(target, "session", session);
            org.powermock.reflect.Whitebox.setInternalState(target, "saveProcessSucceed", false);
            org.powermock.reflect.Whitebox.setInternalState(target, "action", "MODIFY_ACTION");
    
            org.powermock.api.mockito.PowerMockito.doNothing().when(target, "checkIsPendingEbAcceptance");
            org.powermock.api.mockito.PowerMockito.doNothing().when(target, "saveActiveMa_PendingForApproval_noDnaChanges");
            org.powermock.api.mockito.PowerMockito.doReturn(-1L).when(target, "validate");
            org.powermock.api.mockito.PowerMockito.doReturn(true).when(target, "hasPermission", Mockito.anyString(), Mockito.anyBoolean());
            org.powermock.api.mockito.PowerMockito.doReturn(String.valueOf(com.traiana.bundle.core.codes.MasterAgreementStatusInterface.K_INACTIVE)).when(target, "retrieveMaStatus");
            org.powermock.api.mockito.PowerMockito.doNothing().when(target, "saveInactiveMa");
    
            org.mockito.Mockito.when(maBean.getModifyProcessState()).thenReturn(com.traiana.bundle.setup.dma.DnmFinals.MA_MODIFY_STATE.SAVE_ALL_CHANGES_IN_DB_NO_DNA_CHANGES);
    
            org.powermock.reflect.Whitebox.invokeMethod(target, "work");
    
            org.powermock.api.mockito.PowerMockito.verifyPrivate(target).invoke("saveInactiveMa");
            org.mockito.Mockito.verify(session).setAttribute(com.traiana.bundle.setup.dma.DnmFinals.NEW_FLAG, "false");
            org.mockito.Mockito.verify(maBean).setDisplayMessage(String.valueOf(com.traiana.bundle.core.FsLogFinals.MA_MODIFIEF_SUCCESSFULLY));
        }

        @org.junit.Test
        @org.powermock.core.classloader.annotations.PrepareForTest(com.traiana.bundle.setup.dma.ma.MaSaveOp.class)
        public void test_work_noSuccess_validateMinusOne_actionModify_hasPermission_statusActive() throws Exception {
            com.traiana.bundle.setup.dma.ma.MaSaveOp target = org.powermock.api.mockito.PowerMockito.mock(com.traiana.bundle.setup.dma.ma.MaSaveOp.class, org.mockito.Mockito.CALLS_REAL_METHODS);
            com.traiana.bundle.fxweb.dna.screen.MasterAgreementBean maBean = org.mockito.Mockito.mock(com.traiana.bundle.fxweb.dna.screen.MasterAgreementBean.class);
            com.traiana.workspace.session.WorkspaceSession session = org.mockito.Mockito.mock(com.traiana.workspace.session.WorkspaceSession.class);
            org.powermock.reflect.Whitebox.setInternalState(target, "maBean", maBean);
            org.powermock.reflect.Whitebox.setInternalState(target, "session", session);
            org.powermock.reflect.Whitebox.setInternalState(target, "saveProcessSucceed", false);
            org.powermock.reflect.Whitebox.setInternalState(target, "action", "MODIFY_ACTION");
    
            org.powermock.api.mockito.PowerMockito.doNothing().when(target, "checkIsPendingEbAcceptance");
            org.powermock.api.mockito.PowerMockito.doReturn(-1L).when(target, "validate");
            org.powermock.api.mockito.PowerMockito.doReturn(true).when(target, "hasPermission", Mockito.anyString(), Mockito.anyBoolean());
            org.powermock.api.mockito.PowerMockito.doReturn("ACTIVE_STATUS").when(target, "retrieveMaStatus");
            org.powermock.api.mockito.PowerMockito.doNothing().when(target, "saveActiveMa");
    
            org.mockito.Mockito.when(maBean.getModifyProcessState()).thenReturn(com.traiana.bundle.setup.dma.DnmFinals.MA_MODIFY_STATE.NA);
    
            org.powermock.reflect.Whitebox.invokeMethod(target, "work");
    
            org.powermock.api.mockito.PowerMockito.verifyPrivate(target).invoke("saveActiveMa");
        }

        @org.junit.Test
        @org.powermock.core.classloader.annotations.PrepareForTest(com.traiana.bundle.setup.dma.ma.MaSaveOp.class)
        public void test_work_noSuccess_validateMinusOne_actionSave_noPermission_throwsException() throws Exception {
            com.traiana.bundle.setup.dma.ma.MaSaveOp target = org.powermock.api.mockito.PowerMockito.mock(com.traiana.bundle.setup.dma.ma.MaSaveOp.class, org.mockito.Mockito.CALLS_REAL_METHODS);
            com.traiana.bundle.fxweb.dna.screen.MasterAgreementBean maBean = org.mockito.Mockito.mock(com.traiana.bundle.fxweb.dna.screen.MasterAgreementBean.class);
            org.powermock.reflect.Whitebox.setInternalState(target, "maBean", maBean);
            org.powermock.reflect.Whitebox.setInternalState(target, "saveProcessSucceed", false);
            org.powermock.reflect.Whitebox.setInternalState(target, "action", com.traiana.bundle.setup.dma.DnmFinals.DNM_ACTION_SAVE);
    
            org.powermock.api.mockito.PowerMockito.doNothing().when(target, "checkIsPendingEbAcceptance");
            org.powermock.api.mockito.PowerMockito.doReturn(-1L).when(target, "validate");
            org.powermock.api.mockito.PowerMockito.doReturn(false).when(target, "hasPermission", Mockito.anyString(), Mockito.anyBoolean());
    
            org.mockito.Mockito.when(maBean.getModifyProcessState()).thenReturn(com.traiana.bundle.setup.dma.DnmFinals.MA_MODIFY_STATE.NA);
    
            try {
                org.powermock.reflect.Whitebox.invokeMethod(target, "work");
                org.junit.Assert.fail("Expected AppBeanException");
            } catch (Exception e) {
                String name = e.getClass().getName();
                if (e.getCause() != null) {
                    name = e.getCause().getClass().getName();
                }
                org.junit.Assert.assertEquals("com.traiana.workspace.bean.AppBeanException", name);
            }
        }

        @org.junit.Test
        @org.powermock.core.classloader.annotations.PrepareForTest(com.traiana.bundle.setup.dma.ma.MaSaveOp.class)
        public void test_work_noSuccess_validateNotMinusOne_throwsException() throws Exception {
            com.traiana.bundle.setup.dma.ma.MaSaveOp target = org.powermock.api.mockito.PowerMockito.mock(com.traiana.bundle.setup.dma.ma.MaSaveOp.class, org.mockito.Mockito.CALLS_REAL_METHODS);
            com.traiana.bundle.fxweb.dna.screen.MasterAgreementBean maBean = org.mockito.Mockito.mock(com.traiana.bundle.fxweb.dna.screen.MasterAgreementBean.class);
            org.powermock.reflect.Whitebox.setInternalState(target, "maBean", maBean);
            org.powermock.reflect.Whitebox.setInternalState(target, "saveProcessSucceed", false);
            org.powermock.reflect.Whitebox.setInternalState(target, "action", "MODIFY");
    
            org.powermock.api.mockito.PowerMockito.doNothing().when(target, "checkIsPendingEbAcceptance");
            org.powermock.api.mockito.PowerMockito.doReturn(100L).when(target, "validate");
    
            org.mockito.Mockito.when(maBean.getModifyProcessState()).thenReturn(com.traiana.bundle.setup.dma.DnmFinals.MA_MODIFY_STATE.NA);
    
            try {
                org.powermock.reflect.Whitebox.invokeMethod(target, "work");
                org.junit.Assert.fail("Expected AppBeanException");
            } catch (Exception e) {
                String name = e.getClass().getName();
                if (e.getCause() != null) {
                    name = e.getCause().getClass().getName();
                }
                org.junit.Assert.assertEquals("com.traiana.workspace.bean.AppBeanException", name);
            }
        }

        @org.junit.Test
        @org.powermock.core.classloader.annotations.PrepareForTest(com.traiana.bundle.setup.dma.ma.MaSaveOp.class)
        public void test_work_saveProcessSucceed_pbAcceptance_notApprovedInPass_active() throws Exception {
            com.traiana.bundle.setup.dma.ma.MaSaveOp target = org.powermock.api.mockito.PowerMockito.mock(com.traiana.bundle.setup.dma.ma.MaSaveOp.class, org.mockito.Mockito.CALLS_REAL_METHODS);
            com.traiana.bundle.fxweb.dna.screen.MasterAgreementBean maBean = org.mockito.Mockito.mock(com.traiana.bundle.fxweb.dna.screen.MasterAgreementBean.class);
            com.traiana.bundle.core.session.AppWorkspaceSession sessionImpl = org.mockito.Mockito.mock(com.traiana.bundle.core.session.AppWorkspaceSession.class);
            org.powermock.reflect.Whitebox.setInternalState(target, "maBean", maBean);
            org.powermock.reflect.Whitebox.setInternalState(target, "sessionImpl", sessionImpl);
            org.powermock.reflect.Whitebox.setInternalState(target, "saveProcessSucceed", true);
            org.powermock.reflect.Whitebox.setInternalState(target, "action", "MODIFY");
    
            org.powermock.api.mockito.PowerMockito.doNothing().when(target, "checkIsPendingEbAcceptance");
            org.powermock.api.mockito.PowerMockito.doNothing().when(target, "clearApprovalList");
            org.powermock.api.mockito.PowerMockito.doNothing().when(target, "prepareMessage");
            org.powermock.api.mockito.PowerMockito.doNothing().when(target, "updateAppovalMaStatus");
    
            org.mockito.Mockito.when(maBean.getModifyProcessState()).thenReturn(com.traiana.bundle.setup.dma.DnmFinals.MA_MODIFY_STATE.NA);
            org.mockito.Mockito.when(maBean.isPbAcceptance_()).thenReturn(true);
            org.mockito.Mockito.when(maBean.isApprovedInPass()).thenReturn(false);
            org.mockito.Mockito.when(maBean.getStatus()).thenReturn("999");
    
            org.powermock.reflect.Whitebox.invokeMethod(target, "work");
    
            org.mockito.Mockito.verify(maBean).setApprovalStatusAndNextApprovingEntity(String.valueOf(com.traiana.bundle.core.codes.MaApprovalStatusConstsInterface.K_PENDING_APPROVAL), com.traiana.bundle.setup.dma.DnmFinals.NEXT_APPRVING_ENTITY.PB);
            org.mockito.Mockito.verify(sessionImpl).setAttribute(com.traiana.bundle.setup.dma.DnmFinals.MA_STATUS, com.traiana.bundle.core.codes.MasterAgreementStatusInterface.V_ACTIVE);
            org.powermock.api.mockito.PowerMockito.verifyPrivate(target).invoke("updateAppovalMaStatus");
        }

        @org.junit.Test
        @org.powermock.core.classloader.annotations.PrepareForTest(com.traiana.bundle.setup.dma.ma.MaSaveOp.class)
        public void test_work_saveProcessSucceed_notPbAcceptance_actionSave() throws Exception {
            com.traiana.bundle.setup.dma.ma.MaSaveOp target = org.powermock.api.mockito.PowerMockito.mock(com.traiana.bundle.setup.dma.ma.MaSaveOp.class, org.mockito.Mockito.CALLS_REAL_METHODS);
            com.traiana.bundle.fxweb.dna.screen.MasterAgreementBean maBean = org.mockito.Mockito.mock(com.traiana.bundle.fxweb.dna.screen.MasterAgreementBean.class);
            com.traiana.workspace.session.WorkspaceSession session = org.mockito.Mockito.mock(com.traiana.workspace.session.WorkspaceSession.class);
            org.powermock.reflect.Whitebox.setInternalState(target, "maBean", maBean);
            org.powermock.reflect.Whitebox.setInternalState(target, "session", session);
            org.powermock.reflect.Whitebox.setInternalState(target, "saveProcessSucceed", true);
            org.powermock.reflect.Whitebox.setInternalState(target, "action", com.traiana.bundle.setup.dma.DnmFinals.DNM_ACTION_SAVE);
    
            org.powermock.api.mockito.PowerMockito.doNothing().when(target, "checkIsPendingEbAcceptance");
            org.powermock.api.mockito.PowerMockito.doNothing().when(target, "approveMA");
            org.powermock.api.mockito.PowerMockito.doNothing().when(target, "prepareMessage");
    
            org.mockito.Mockito.when(maBean.getModifyProcessState()).thenReturn(com.traiana.bundle.setup.dma.DnmFinals.MA_MODIFY_STATE.NA);
            org.mockito.Mockito.when(maBean.isPbAcceptance_()).thenReturn(false);
    
            org.powermock.reflect.Whitebox.invokeMethod(target, "work");
    
            org.mockito.Mockito.verify(session).setAttribute(com.traiana.bundle.setup.dma.DnmFinals.NEW_FLAG, "true");
            org.powermock.api.mockito.PowerMockito.verifyPrivate(target).invoke("approveMA");
            org.powermock.api.mockito.PowerMockito.verifyPrivate(target).invoke("prepareMessage");
        }

        @org.junit.Test
        @org.powermock.core.classloader.annotations.PrepareForTest(com.traiana.bundle.setup.dma.ma.MaSaveOp.class)
        public void test_work_catchesUtilException_throwsAppBeanException() throws Exception {
            com.traiana.bundle.setup.dma.ma.MaSaveOp target = org.powermock.api.mockito.PowerMockito.mock(com.traiana.bundle.setup.dma.ma.MaSaveOp.class, org.mockito.Mockito.CALLS_REAL_METHODS);
            com.traiana.workspace.util.UtilException ex = org.mockito.Mockito.mock(com.traiana.workspace.util.UtilException.class);
            org.powermock.api.mockito.PowerMockito.doAnswer(new org.mockito.stubbing.Answer<Void>() {
                public Void answer(org.mockito.invocation.InvocationOnMock invocation) throws Throwable {
                    throw ex;
                }
            }).when(target, "checkIsPendingEbAcceptance");
    
            try {
                org.powermock.reflect.Whitebox.invokeMethod(target, "work");
                org.junit.Assert.fail("Expected AppBeanException");
            } catch (Exception e) {
                String name = e.getClass().getName();
                if (e.getCause() != null) {
                    name = e.getCause().getClass().getName();
                }
                org.junit.Assert.assertEquals("com.traiana.workspace.bean.AppBeanException", name);
            }
        }

        @org.junit.Test
        @org.powermock.core.classloader.annotations.PrepareForTest(com.traiana.bundle.setup.dma.ma.MaSaveOp.class)
        public void test_work_catchesGeneralException_throwsAppBeanException() throws Exception {
            com.traiana.bundle.setup.dma.ma.MaSaveOp target = org.powermock.api.mockito.PowerMockito.mock(com.traiana.bundle.setup.dma.ma.MaSaveOp.class, org.mockito.Mockito.CALLS_REAL_METHODS);
            org.powermock.api.mockito.PowerMockito.doAnswer(new org.mockito.stubbing.Answer<Void>() {
                public Void answer(org.mockito.invocation.InvocationOnMock invocation) throws Throwable {
                    throw new java.lang.Exception("general error");
                }
            }).when(target, "checkIsPendingEbAcceptance");
    
            try {
                org.powermock.reflect.Whitebox.invokeMethod(target, "work");
                org.junit.Assert.fail("Expected AppBeanException");
            } catch (Exception e) {
                String name = e.getClass().getName();
                if (e.getCause() != null) {
                    name = e.getCause().getClass().getName();
                }
                org.junit.Assert.assertEquals("com.traiana.workspace.bean.AppBeanException", name);
            }
        }

        @Test
        public void test_populateNarrowCurrenciesSB_FXCashDeliverable_Success() throws Exception {
            com.traiana.bundle.setup.dma.ma.MaSaveOp target = new com.traiana.bundle.setup.dma.ma.MaSaveOp();
            int currenciesGroup = com.traiana.bundle.setup.dma.DnaReportFinals.PRODUCT_IDS.FX_CASH_DELIVERABLE;
            String title = "FX Cash";
            List<String> oldSelectedList = Arrays.asList("EUR", "GBP");
            List<String> selectedList = Arrays.asList("EUR");
            StringBuilder narrowCurrenciesSB = new StringBuilder();
            com.traiana.bundle.fxweb.dna.screen.NarrowChanges narrowChanges = Mockito.mock(com.traiana.bundle.fxweb.dna.screen.NarrowChanges.class);
    
            Whitebox.invokeMethod(target, "populateNarrowCurrenciesSB", currenciesGroup, title, oldSelectedList, selectedList, narrowCurrenciesSB, narrowChanges);
    
            Mockito.verify(narrowChanges).addCurrency_FXCash("GBP");
            Assert.assertEquals("\n    FX Cash:\n         GBP", narrowCurrenciesSB.toString());
        }

        @Test
        public void test_populateNarrowCurrenciesSB_FXOptionsDeliverable() throws Exception {
            com.traiana.bundle.setup.dma.ma.MaSaveOp target = new com.traiana.bundle.setup.dma.ma.MaSaveOp();
            int currenciesGroup = com.traiana.bundle.setup.dma.DnaReportFinals.PRODUCT_IDS.FX_OPTIONS_DELIVERABLE;
            List<String> oldSelectedList = Arrays.asList("USD");
            List<String> selectedList = new ArrayList<String>();
            StringBuilder narrowCurrenciesSB = new StringBuilder();
            com.traiana.bundle.fxweb.dna.screen.NarrowChanges narrowChanges = Mockito.mock(com.traiana.bundle.fxweb.dna.screen.NarrowChanges.class);
    
            Whitebox.invokeMethod(target, "populateNarrowCurrenciesSB", currenciesGroup, "Title", oldSelectedList, selectedList, narrowCurrenciesSB, narrowChanges);
    
            Mockito.verify(narrowChanges).addCurrency_FXOptions("USD");
            Assert.assertEquals("\n    Title:\n         USD", narrowCurrenciesSB.toString());
        }

        @Test
        public void test_populateNarrowCurrenciesSB_FXCashNDF() throws Exception {
            com.traiana.bundle.setup.dma.ma.MaSaveOp target = new com.traiana.bundle.setup.dma.ma.MaSaveOp();
            int currenciesGroup = com.traiana.bundle.setup.dma.DnaReportFinals.PRODUCT_IDS.FX_CASH_NDF;
            List<String> oldSelectedList = Arrays.asList("AUD");
            List<String> selectedList = new ArrayList<String>();
            StringBuilder narrowCurrenciesSB = new StringBuilder();
            com.traiana.bundle.fxweb.dna.screen.NarrowChanges narrowChanges = Mockito.mock(com.traiana.bundle.fxweb.dna.screen.NarrowChanges.class);
    
            Whitebox.invokeMethod(target, "populateNarrowCurrenciesSB", currenciesGroup, "NDF", oldSelectedList, selectedList, narrowCurrenciesSB, narrowChanges);
    
            Mockito.verify(narrowChanges).addCurrency_NDF("AUD");
            Assert.assertEquals("\n    NDF:\n         AUD", narrowCurrenciesSB.toString());
        }

        @Test
        public void test_populateNarrowCurrenciesSB_FXOptionsNDO() throws Exception {
            com.traiana.bundle.setup.dma.ma.MaSaveOp target = new com.traiana.bundle.setup.dma.ma.MaSaveOp();
            int currenciesGroup = com.traiana.bundle.setup.dma.DnaReportFinals.PRODUCT_IDS.FX_OPTIONS_NDO;
            List<String> oldSelectedList = Arrays.asList("CHF");
            List<String> selectedList = new ArrayList<String>();
            StringBuilder narrowCurrenciesSB = new StringBuilder();
            com.traiana.bundle.fxweb.dna.screen.NarrowChanges narrowChanges = Mockito.mock(com.traiana.bundle.fxweb.dna.screen.NarrowChanges.class);
    
            Whitebox.invokeMethod(target, "populateNarrowCurrenciesSB", currenciesGroup, "NDO", oldSelectedList, selectedList, narrowCurrenciesSB, narrowChanges);
    
            Mockito.verify(narrowChanges).addCurrency_NDO("CHF");
        }

        @Test
        public void test_populateNarrowCurrenciesSB_Bullion() throws Exception {
            com.traiana.bundle.setup.dma.ma.MaSaveOp target = new com.traiana.bundle.setup.dma.ma.MaSaveOp();
            int currenciesGroup = com.traiana.bundle.setup.dma.DnaReportFinals.PRODUCT_IDS.BULLION;
            List<String> oldSelectedList = Arrays.asList("XAU");
            List<String> selectedList = new ArrayList<String>();
            StringBuilder narrowCurrenciesSB = new StringBuilder();
            com.traiana.bundle.fxweb.dna.screen.NarrowChanges narrowChanges = Mockito.mock(com.traiana.bundle.fxweb.dna.screen.NarrowChanges.class);
    
            Whitebox.invokeMethod(target, "populateNarrowCurrenciesSB", currenciesGroup, "Bullion", oldSelectedList, selectedList, narrowCurrenciesSB, narrowChanges);
    
            Mockito.verify(narrowChanges).addCurrency_Bullion("XAU");
        }

        @Test
        public void test_populateNarrowCurrenciesSB_BullionCcyPair() throws Exception {
            com.traiana.bundle.setup.dma.ma.MaSaveOp target = new com.traiana.bundle.setup.dma.ma.MaSaveOp();
            int currenciesGroup = com.traiana.bundle.setup.dma.DnaReportFinals.PRODUCT_IDS.BULLION_CCY_PAIR;
            List<String> oldSelectedList = Arrays.asList("XAG/USD");
            List<String> selectedList = new ArrayList<String>();
            StringBuilder narrowCurrenciesSB = new StringBuilder();
            com.traiana.bundle.fxweb.dna.screen.NarrowChanges narrowChanges = Mockito.mock(com.traiana.bundle.fxweb.dna.screen.NarrowChanges.class);
    
            Whitebox.invokeMethod(target, "populateNarrowCurrenciesSB", currenciesGroup, "Bullion Pair", oldSelectedList, selectedList, narrowCurrenciesSB, narrowChanges);
    
            Mockito.verify(narrowChanges).addCurrency_Bullion_ccyPairs("XAG/USD");
        }

        @Test
        public void test_populateNarrowCurrenciesSB_ExoticOptions() throws Exception {
            com.traiana.bundle.setup.dma.ma.MaSaveOp target = new com.traiana.bundle.setup.dma.ma.MaSaveOp();
            int currenciesGroup = com.traiana.bundle.setup.dma.DnaReportFinals.PRODUCT_IDS.EXOTIC_OPTIONS;
            List<String> oldSelectedList = Arrays.asList("TRY");
            List<String> selectedList = new ArrayList<String>();
            StringBuilder narrowCurrenciesSB = new StringBuilder();
            com.traiana.bundle.fxweb.dna.screen.NarrowChanges narrowChanges = Mockito.mock(com.traiana.bundle.fxweb.dna.screen.NarrowChanges.class);
    
            Whitebox.invokeMethod(target, "populateNarrowCurrenciesSB", currenciesGroup, "Exotic Options", oldSelectedList, selectedList, narrowCurrenciesSB, narrowChanges);
    
            Mockito.verify(narrowChanges).addCurrency_ExcticOptions("TRY");
        }

        @Test
        public void test_populateNarrowCurrenciesSB_ExoticNDO() throws Exception {
            com.traiana.bundle.setup.dma.ma.MaSaveOp target = new com.traiana.bundle.setup.dma.ma.MaSaveOp();
            int currenciesGroup = com.traiana.bundle.setup.dma.DnaReportFinals.PRODUCT_IDS.EXOTIC_NDO;
            List<String> oldSelectedList = Arrays.asList("ZAR");
            List<String> selectedList = new ArrayList<String>();
            StringBuilder narrowCurrenciesSB = new StringBuilder();
            com.traiana.bundle.fxweb.dna.screen.NarrowChanges narrowChanges = Mockito.mock(com.traiana.bundle.fxweb.dna.screen.NarrowChanges.class);
    
            Whitebox.invokeMethod(target, "populateNarrowCurrenciesSB", currenciesGroup, "Exotic NDO", oldSelectedList, selectedList, narrowCurrenciesSB, narrowChanges);
    
            Mockito.verify(narrowChanges).addCurrency_ExcticNDO("ZAR");
        }

        @Test
        public void test_populateNarrowCurrenciesSB_UnknownGroup() throws Exception {
            com.traiana.bundle.setup.dma.ma.MaSaveOp target = new com.traiana.bundle.setup.dma.ma.MaSaveOp();
            int currenciesGroup = -1;
            List<String> oldSelectedList = Arrays.asList("JPY", "CAD");
            List<String> selectedList = new ArrayList<String>();
            StringBuilder narrowCurrenciesSB = new StringBuilder();
            com.traiana.bundle.fxweb.dna.screen.NarrowChanges narrowChanges = Mockito.mock(com.traiana.bundle.fxweb.dna.screen.NarrowChanges.class);
    
            Whitebox.invokeMethod(target, "populateNarrowCurrenciesSB", currenciesGroup, "Unknown", oldSelectedList, selectedList, narrowCurrenciesSB, narrowChanges);
    
            Mockito.verifyZeroInteractions(narrowChanges);
            Assert.assertEquals("\n    Unknown:\n         JPY,CAD", narrowCurrenciesSB.toString());
        }

        @Test
        public void test_populateNarrowCurrenciesSB_EmptyLists() throws Exception {
            com.traiana.bundle.setup.dma.ma.MaSaveOp target = new com.traiana.bundle.setup.dma.ma.MaSaveOp();
            int currenciesGroup = 1;
            List<String> oldSelectedList = new ArrayList<String>();
            List<String> selectedList = new ArrayList<String>();
            StringBuilder narrowCurrenciesSB = new StringBuilder();
            com.traiana.bundle.fxweb.dna.screen.NarrowChanges narrowChanges = Mockito.mock(com.traiana.bundle.fxweb.dna.screen.NarrowChanges.class);
    
            Whitebox.invokeMethod(target, "populateNarrowCurrenciesSB", currenciesGroup, "Empty", oldSelectedList, selectedList, narrowCurrenciesSB, narrowChanges);
    
            Mockito.verifyZeroInteractions(narrowChanges);
            Assert.assertEquals("", narrowCurrenciesSB.toString());
        }

        @Test
        public void test_populateNarrowCurrenciesSB_NoChanges() throws Exception {
            com.traiana.bundle.setup.dma.ma.MaSaveOp target = new com.traiana.bundle.setup.dma.ma.MaSaveOp();
            int currenciesGroup = 999;
            List<String> oldSelectedList = Arrays.asList("USD");
            List<String> selectedList = Arrays.asList("USD");
            StringBuilder narrowCurrenciesSB = new StringBuilder();
            com.traiana.bundle.fxweb.dna.screen.NarrowChanges narrowChanges = Mockito.mock(com.traiana.bundle.fxweb.dna.screen.NarrowChanges.class);
    
            Whitebox.invokeMethod(target, "populateNarrowCurrenciesSB", currenciesGroup, "No Change", oldSelectedList, selectedList, narrowCurrenciesSB, narrowChanges);
    
            Mockito.verifyZeroInteractions(narrowChanges);
            Assert.assertEquals("", narrowCurrenciesSB.toString());
        }

        @Test
        public void test_emptySelectedList_noChanges() throws Exception {
            com.traiana.bundle.setup.dma.ma.MaSaveOp target = new com.traiana.bundle.setup.dma.ma.MaSaveOp();
            java.util.List<String> oldList = java.util.Arrays.asList("USD");
            java.util.List<String> newList = new java.util.ArrayList<>();
            com.traiana.bundle.fxweb.dna.screen.ExpandChanges expandChanges = org.mockito.Mockito.mock(com.traiana.bundle.fxweb.dna.screen.ExpandChanges.class);
    
            org.powermock.reflect.Whitebox.invokeMethod(target, "populateExpandCurrencies2ExpandChanges", com.traiana.bundle.setup.dma.DnaReportFinals.PRODUCT_IDS.FX_CASH_DELIVERABLE, oldList, newList, expandChanges);
    
            org.mockito.Mockito.verifyZeroInteractions(expandChanges);
        }

        @Test
        public void test_existingCurrency_noChanges() throws Exception {
            com.traiana.bundle.setup.dma.ma.MaSaveOp target = new com.traiana.bundle.setup.dma.ma.MaSaveOp();
            java.util.List<String> oldList = java.util.Arrays.asList("USD");
            java.util.List<String> newList = java.util.Arrays.asList("USD");
            com.traiana.bundle.fxweb.dna.screen.ExpandChanges expandChanges = org.mockito.Mockito.mock(com.traiana.bundle.fxweb.dna.screen.ExpandChanges.class);
    
            org.powermock.reflect.Whitebox.invokeMethod(target, "populateExpandCurrencies2ExpandChanges", com.traiana.bundle.setup.dma.DnaReportFinals.PRODUCT_IDS.FX_CASH_DELIVERABLE, oldList, newList, expandChanges);
    
            org.mockito.Mockito.verifyZeroInteractions(expandChanges);
        }

        @Test
        public void test_unmatchedGroup_noChanges() throws Exception {
            com.traiana.bundle.setup.dma.ma.MaSaveOp target = new com.traiana.bundle.setup.dma.ma.MaSaveOp();
            java.util.List<String> oldList = new java.util.ArrayList<>();
            java.util.List<String> newList = java.util.Arrays.asList("CAD");
            com.traiana.bundle.fxweb.dna.screen.ExpandChanges expandChanges = org.mockito.Mockito.mock(com.traiana.bundle.fxweb.dna.screen.ExpandChanges.class);
    
            org.powermock.reflect.Whitebox.invokeMethod(target, "populateExpandCurrencies2ExpandChanges", -999, oldList, newList, expandChanges);
    
            org.mockito.Mockito.verifyZeroInteractions(expandChanges);
        }

        @Test
        public void test_fxCashDeliverable() throws Exception {
            com.traiana.bundle.setup.dma.ma.MaSaveOp target = new com.traiana.bundle.setup.dma.ma.MaSaveOp();
            java.util.List<String> oldList = new java.util.ArrayList<>();
            java.util.List<String> newList = java.util.Arrays.asList("EUR");
            com.traiana.bundle.fxweb.dna.screen.ExpandChanges expandChanges = org.mockito.Mockito.mock(com.traiana.bundle.fxweb.dna.screen.ExpandChanges.class);
    
            org.powermock.reflect.Whitebox.invokeMethod(target, "populateExpandCurrencies2ExpandChanges", com.traiana.bundle.setup.dma.DnaReportFinals.PRODUCT_IDS.FX_CASH_DELIVERABLE, oldList, newList, expandChanges);
    
            org.mockito.Mockito.verify(expandChanges).addCurrency_FXCash(org.mockito.Mockito.eq("EUR"), org.mockito.Mockito.eq(Boolean.FALSE));
        }

        @Test
        public void test_fxOptionsDeliverable() throws Exception {
            com.traiana.bundle.setup.dma.ma.MaSaveOp target = new com.traiana.bundle.setup.dma.ma.MaSaveOp();
            java.util.List<String> oldList = new java.util.ArrayList<>();
            java.util.List<String> newList = java.util.Arrays.asList("EUR");
            com.traiana.bundle.fxweb.dna.screen.ExpandChanges expandChanges = org.mockito.Mockito.mock(com.traiana.bundle.fxweb.dna.screen.ExpandChanges.class);
    
            org.powermock.reflect.Whitebox.invokeMethod(target, "populateExpandCurrencies2ExpandChanges", com.traiana.bundle.setup.dma.DnaReportFinals.PRODUCT_IDS.FX_OPTIONS_DELIVERABLE, oldList, newList, expandChanges);
    
            org.mockito.Mockito.verify(expandChanges).addCurrency_FXOptions(org.mockito.Mockito.eq("EUR"), org.mockito.Mockito.eq(Boolean.FALSE));
        }

        @Test
        public void test_fxCashNdf() throws Exception {
            com.traiana.bundle.setup.dma.ma.MaSaveOp target = new com.traiana.bundle.setup.dma.ma.MaSaveOp();
            java.util.List<String> oldList = new java.util.ArrayList<>();
            java.util.List<String> newList = java.util.Arrays.asList("EUR");
            com.traiana.bundle.fxweb.dna.screen.ExpandChanges expandChanges = org.mockito.Mockito.mock(com.traiana.bundle.fxweb.dna.screen.ExpandChanges.class);
    
            org.powermock.reflect.Whitebox.invokeMethod(target, "populateExpandCurrencies2ExpandChanges", com.traiana.bundle.setup.dma.DnaReportFinals.PRODUCT_IDS.FX_CASH_NDF, oldList, newList, expandChanges);
    
            org.mockito.Mockito.verify(expandChanges).addCurrency_NDF(org.mockito.Mockito.eq("EUR"), org.mockito.Mockito.eq(Boolean.FALSE));
        }

        @Test
        public void test_fxOptionsNdo() throws Exception {
            com.traiana.bundle.setup.dma.ma.MaSaveOp target = new com.traiana.bundle.setup.dma.ma.MaSaveOp();
            java.util.List<String> oldList = new java.util.ArrayList<>();
            java.util.List<String> newList = java.util.Arrays.asList("EUR");
            com.traiana.bundle.fxweb.dna.screen.ExpandChanges expandChanges = org.mockito.Mockito.mock(com.traiana.bundle.fxweb.dna.screen.ExpandChanges.class);
    
            org.powermock.reflect.Whitebox.invokeMethod(target, "populateExpandCurrencies2ExpandChanges", com.traiana.bundle.setup.dma.DnaReportFinals.PRODUCT_IDS.FX_OPTIONS_NDO, oldList, newList, expandChanges);
    
            org.mockito.Mockito.verify(expandChanges).addCurrency_NDO(org.mockito.Mockito.eq("EUR"), org.mockito.Mockito.eq(Boolean.FALSE));
        }

        @Test
        public void test_bullion() throws Exception {
            com.traiana.bundle.setup.dma.ma.MaSaveOp target = new com.traiana.bundle.setup.dma.ma.MaSaveOp();
            java.util.List<String> oldList = new java.util.ArrayList<>();
            java.util.List<String> newList = java.util.Arrays.asList("EUR");
            com.traiana.bundle.fxweb.dna.screen.ExpandChanges expandChanges = org.mockito.Mockito.mock(com.traiana.bundle.fxweb.dna.screen.ExpandChanges.class);
    
            org.powermock.reflect.Whitebox.invokeMethod(target, "populateExpandCurrencies2ExpandChanges", com.traiana.bundle.setup.dma.DnaReportFinals.PRODUCT_IDS.BULLION, oldList, newList, expandChanges);
    
            org.mockito.Mockito.verify(expandChanges).addCurrency_Bullion(org.mockito.Mockito.eq("EUR"), org.mockito.Mockito.eq(Boolean.FALSE));
        }

        @Test
        public void test_bullionCcyPair() throws Exception {
            com.traiana.bundle.setup.dma.ma.MaSaveOp target = new com.traiana.bundle.setup.dma.ma.MaSaveOp();
            java.util.List<String> oldList = new java.util.ArrayList<>();
            java.util.List<String> newList = java.util.Arrays.asList("EUR");
            com.traiana.bundle.fxweb.dna.screen.ExpandChanges expandChanges = org.mockito.Mockito.mock(com.traiana.bundle.fxweb.dna.screen.ExpandChanges.class);
    
            org.powermock.reflect.Whitebox.invokeMethod(target, "populateExpandCurrencies2ExpandChanges", com.traiana.bundle.setup.dma.DnaReportFinals.PRODUCT_IDS.BULLION_CCY_PAIR, oldList, newList, expandChanges);
    
            org.mockito.Mockito.verify(expandChanges).addCurrency_Bullion_ccyPairs(org.mockito.Mockito.eq("EUR"), org.mockito.Mockito.eq(Boolean.FALSE));
        }

        @Test
        public void test_exoticOptions() throws Exception {
            com.traiana.bundle.setup.dma.ma.MaSaveOp target = new com.traiana.bundle.setup.dma.ma.MaSaveOp();
            java.util.List<String> oldList = new java.util.ArrayList<>();
            java.util.List<String> newList = java.util.Arrays.asList("EUR");
            com.traiana.bundle.fxweb.dna.screen.ExpandChanges expandChanges = org.mockito.Mockito.mock(com.traiana.bundle.fxweb.dna.screen.ExpandChanges.class);
    
            org.powermock.reflect.Whitebox.invokeMethod(target, "populateExpandCurrencies2ExpandChanges", com.traiana.bundle.setup.dma.DnaReportFinals.PRODUCT_IDS.EXOTIC_OPTIONS, oldList, newList, expandChanges);
    
            org.mockito.Mockito.verify(expandChanges).addCurrency_ExcticOptions(org.mockito.Mockito.eq("EUR"), org.mockito.Mockito.eq(Boolean.FALSE));
        }

        @Test
        public void test_exoticNdo() throws Exception {
            com.traiana.bundle.setup.dma.ma.MaSaveOp target = new com.traiana.bundle.setup.dma.ma.MaSaveOp();
            java.util.List<String> oldList = new java.util.ArrayList<>();
            java.util.List<String> newList = java.util.Arrays.asList("EUR");
            com.traiana.bundle.fxweb.dna.screen.ExpandChanges expandChanges = org.mockito.Mockito.mock(com.traiana.bundle.fxweb.dna.screen.ExpandChanges.class);
    
            org.powermock.reflect.Whitebox.invokeMethod(target, "populateExpandCurrencies2ExpandChanges", com.traiana.bundle.setup.dma.DnaReportFinals.PRODUCT_IDS.EXOTIC_NDO, oldList, newList, expandChanges);
    
            org.mockito.Mockito.verify(expandChanges).addCurrency_ExcticNDO(org.mockito.Mockito.eq("EUR"), org.mockito.Mockito.eq(Boolean.FALSE));
        }

        @org.junit.Test
        public void test_populateExpandCurrencies2ExpandChanges_allFalse_returnsEmptyMap() throws Exception {
            com.traiana.bundle.setup.dma.ma.MaSaveOp target = new com.traiana.bundle.setup.dma.ma.MaSaveOp();
            com.traiana.bundle.setup.dma.ma.MaSaveOp spyTarget = org.powermock.api.mockito.PowerMockito.spy(target);
            
            com.traiana.bundle.fxweb.dna.screen.MasterAgreementBean oldMasterAgreementBean = org.mockito.Mockito.mock(com.traiana.bundle.fxweb.dna.screen.MasterAgreementBean.class);
            com.traiana.bundle.fxweb.dna.screen.MasterAgreementBean maBean = org.mockito.Mockito.mock(com.traiana.bundle.fxweb.dna.screen.MasterAgreementBean.class);
            com.traiana.bundle.fxweb.dna.screen.ExpandChanges expandChanges = org.mockito.Mockito.mock(com.traiana.bundle.fxweb.dna.screen.ExpandChanges.class);
            
            org.powermock.reflect.Whitebox.setInternalState(spyTarget, "maBean", maBean);
            
            java.util.List dummyList = new java.util.ArrayList();
            org.mockito.Mockito.when(oldMasterAgreementBean.getFxCashDeliverableSelectedList()).thenReturn(dummyList);
            org.mockito.Mockito.when(oldMasterAgreementBean.getFxOptionsDeliverableSelectedList()).thenReturn(dummyList);
            org.mockito.Mockito.when(oldMasterAgreementBean.getFxCashNDFSelectedList()).thenReturn(dummyList);
            org.mockito.Mockito.when(oldMasterAgreementBean.getFxOptionsNDFSelectedList()).thenReturn(dummyList);
            org.mockito.Mockito.when(oldMasterAgreementBean.getBullionSelectedList()).thenReturn(dummyList);
            org.mockito.Mockito.when(oldMasterAgreementBean.getBullionCcyPairSelectedList()).thenReturn(dummyList);
            org.mockito.Mockito.when(oldMasterAgreementBean.getExoticOptionsSelectedList()).thenReturn(dummyList);
            org.mockito.Mockito.when(oldMasterAgreementBean.getExoticNDOSelectedList()).thenReturn(dummyList);
            
            org.mockito.Mockito.when(maBean.getFxCashDeliverableSelectedList()).thenReturn(dummyList);
            org.mockito.Mockito.when(maBean.getFxOptionsDeliverableSelectedList()).thenReturn(dummyList);
            org.mockito.Mockito.when(maBean.getFxCashNDFSelectedList()).thenReturn(dummyList);
            org.mockito.Mockito.when(maBean.getFxOptionsNDFSelectedList()).thenReturn(dummyList);
            org.mockito.Mockito.when(maBean.getBullionSelectedList()).thenReturn(dummyList);
            org.mockito.Mockito.when(maBean.getBullionCcyPairSelectedList()).thenReturn(dummyList);
            org.mockito.Mockito.when(maBean.getExoticOptionsSelectedList()).thenReturn(dummyList);
            org.mockito.Mockito.when(maBean.getExoticNDOSelectedList()).thenReturn(dummyList);
            
            org.powermock.api.mockito.PowerMockito.doNothing().when(spyTarget, "populateExpandCurrencies2ExpandChanges",
                org.mockito.Mockito.anyInt(),
                org.mockito.Mockito.anyList(),
                org.mockito.Mockito.anyList(),
                org.mockito.Mockito.any(com.traiana.bundle.fxweb.dna.screen.ExpandChanges.class));
                
            org.mockito.Mockito.when(oldMasterAgreementBean.isFxCash()).thenReturn(true);
            org.mockito.Mockito.when(oldMasterAgreementBean.isNdf()).thenReturn(true);
            org.mockito.Mockito.when(oldMasterAgreementBean.isNdoOptions()).thenReturn(true);
            org.mockito.Mockito.when(oldMasterAgreementBean.isVanillaDeliverableFamily()).thenReturn(true);
            org.mockito.Mockito.when(oldMasterAgreementBean.isExoticOptionFamily()).thenReturn(true);
            org.mockito.Mockito.when(oldMasterAgreementBean.isExoticNdo()).thenReturn(true);
            org.mockito.Mockito.when(oldMasterAgreementBean.isBullion()).thenReturn(true);
            
            org.powermock.reflect.Whitebox.invokeMethod(spyTarget, "populateExpandCurrencies2ExpandChanges", oldMasterAgreementBean, expandChanges);
            
            org.mockito.ArgumentCaptor<java.util.Map> mapCaptor = org.mockito.ArgumentCaptor.forClass(java.util.Map.class);
            org.mockito.Mockito.verify(expandChanges).setMastSelectedCurrencyTypeMap(mapCaptor.capture());
            org.junit.Assert.assertTrue(mapCaptor.getValue().isEmpty());
        }

        @org.junit.Test
        public void test_populateExpandCurrencies2ExpandChanges_allTrue_returnsPopulatedMap() throws Exception {
            com.traiana.bundle.setup.dma.ma.MaSaveOp target = new com.traiana.bundle.setup.dma.ma.MaSaveOp();
            com.traiana.bundle.setup.dma.ma.MaSaveOp spyTarget = org.powermock.api.mockito.PowerMockito.spy(target);
            
            com.traiana.bundle.fxweb.dna.screen.MasterAgreementBean oldMasterAgreementBean = org.mockito.Mockito.mock(com.traiana.bundle.fxweb.dna.screen.MasterAgreementBean.class);
            com.traiana.bundle.fxweb.dna.screen.MasterAgreementBean maBean = org.mockito.Mockito.mock(com.traiana.bundle.fxweb.dna.screen.MasterAgreementBean.class);
            com.traiana.bundle.fxweb.dna.screen.ExpandChanges expandChanges = org.mockito.Mockito.mock(com.traiana.bundle.fxweb.dna.screen.ExpandChanges.class);
            
            org.powermock.reflect.Whitebox.setInternalState(spyTarget, "maBean", maBean);
            
            java.util.List dummyList = new java.util.ArrayList();
            org.mockito.Mockito.when(oldMasterAgreementBean.getFxCashDeliverableSelectedList()).thenReturn(dummyList);
            org.mockito.Mockito.when(oldMasterAgreementBean.getFxOptionsDeliverableSelectedList()).thenReturn(dummyList);
            org.mockito.Mockito.when(oldMasterAgreementBean.getFxCashNDFSelectedList()).thenReturn(dummyList);
            org.mockito.Mockito.when(oldMasterAgreementBean.getFxOptionsNDFSelectedList()).thenReturn(dummyList);
            org.mockito.Mockito.when(oldMasterAgreementBean.getBullionSelectedList()).thenReturn(dummyList);
            org.mockito.Mockito.when(oldMasterAgreementBean.getBullionCcyPairSelectedList()).thenReturn(dummyList);
            org.mockito.Mockito.when(oldMasterAgreementBean.getExoticOptionsSelectedList()).thenReturn(dummyList);
            org.mockito.Mockito.when(oldMasterAgreementBean.getExoticNDOSelectedList()).thenReturn(dummyList);
            
            org.mockito.Mockito.when(maBean.getFxCashDeliverableSelectedList()).thenReturn(dummyList);
            org.mockito.Mockito.when(maBean.getFxOptionsDeliverableSelectedList()).thenReturn(dummyList);
            org.mockito.Mockito.when(maBean.getFxCashNDFSelectedList()).thenReturn(dummyList);
            org.mockito.Mockito.when(maBean.getFxOptionsNDFSelectedList()).thenReturn(dummyList);
            org.mockito.Mockito.when(maBean.getBullionSelectedList()).thenReturn(dummyList);
            org.mockito.Mockito.when(maBean.getBullionCcyPairSelectedList()).thenReturn(dummyList);
            org.mockito.Mockito.when(maBean.getExoticOptionsSelectedList()).thenReturn(dummyList);
            org.mockito.Mockito.when(maBean.getExoticNDOSelectedList()).thenReturn(dummyList);
            
            org.powermock.api.mockito.PowerMockito.doNothing().when(spyTarget, "populateExpandCurrencies2ExpandChanges",
                org.mockito.Mockito.anyInt(),
                org.mockito.Mockito.anyList(),
                org.mockito.Mockito.anyList(),
                org.mockito.Mockito.any(com.traiana.bundle.fxweb.dna.screen.ExpandChanges.class));
                
            org.mockito.Mockito.when(oldMasterAgreementBean.isFxCash()).thenReturn(false);
            org.mockito.Mockito.when(oldMasterAgreementBean.isSpotFarward()).thenReturn(false);
            org.mockito.Mockito.when(oldMasterAgreementBean.isFarward()).thenReturn(false);
            org.mockito.Mockito.when(oldMasterAgreementBean.isNdf()).thenReturn(false);
            org.mockito.Mockito.when(oldMasterAgreementBean.isNdoOptions()).thenReturn(false);
            org.mockito.Mockito.when(oldMasterAgreementBean.isVanillaDeliverableFamily()).thenReturn(false);
            org.mockito.Mockito.when(oldMasterAgreementBean.isExoticOptionFamily()).thenReturn(false);
            org.mockito.Mockito.when(oldMasterAgreementBean.isExoticNdo()).thenReturn(false);
            org.mockito.Mockito.when(oldMasterAgreementBean.isBullion()).thenReturn(false);
            org.mockito.Mockito.when(oldMasterAgreementBean.isBullionSpotFarward()).thenReturn(false);
            org.mockito.Mockito.when(oldMasterAgreementBean.isBullionOption()).thenReturn(false);
            org.mockito.Mockito.when(oldMasterAgreementBean.isBullionOptionExotic()).thenReturn(false);
            
            org.powermock.reflect.Whitebox.invokeMethod(spyTarget, "populateExpandCurrencies2ExpandChanges", oldMasterAgreementBean, expandChanges);
            
            org.mockito.ArgumentCaptor<java.util.Map> mapCaptor = org.mockito.ArgumentCaptor.forClass(java.util.Map.class);
            org.mockito.Mockito.verify(expandChanges).setMastSelectedCurrencyTypeMap(mapCaptor.capture());
            org.junit.Assert.assertEquals(8, mapCaptor.getValue().size());
        }

        @org.junit.Test
        public void test_populateExpandCurrencies2ExpandChanges_mixedPaths() throws Exception {
            com.traiana.bundle.setup.dma.ma.MaSaveOp target = new com.traiana.bundle.setup.dma.ma.MaSaveOp();
            com.traiana.bundle.setup.dma.ma.MaSaveOp spyTarget = org.powermock.api.mockito.PowerMockito.spy(target);
            
            com.traiana.bundle.fxweb.dna.screen.MasterAgreementBean oldMasterAgreementBean = org.mockito.Mockito.mock(com.traiana.bundle.fxweb.dna.screen.MasterAgreementBean.class);
            com.traiana.bundle.fxweb.dna.screen.MasterAgreementBean maBean = org.mockito.Mockito.mock(com.traiana.bundle.fxweb.dna.screen.MasterAgreementBean.class);
            com.traiana.bundle.fxweb.dna.screen.ExpandChanges expandChanges = org.mockito.Mockito.mock(com.traiana.bundle.fxweb.dna.screen.ExpandChanges.class);
            
            org.powermock.reflect.Whitebox.setInternalState(spyTarget, "maBean", maBean);
            
            java.util.List dummyList = new java.util.ArrayList();
            org.mockito.Mockito.when(oldMasterAgreementBean.getFxCashDeliverableSelectedList()).thenReturn(dummyList);
            org.mockito.Mockito.when(oldMasterAgreementBean.getFxOptionsDeliverableSelectedList()).thenReturn(dummyList);
            org.mockito.Mockito.when(oldMasterAgreementBean.getFxCashNDFSelectedList()).thenReturn(dummyList);
            org.mockito.Mockito.when(oldMasterAgreementBean.getFxOptionsNDFSelectedList()).thenReturn(dummyList);
            org.mockito.Mockito.when(oldMasterAgreementBean.getBullionSelectedList()).thenReturn(dummyList);
            org.mockito.Mockito.when(oldMasterAgreementBean.getBullionCcyPairSelectedList()).thenReturn(dummyList);
            org.mockito.Mockito.when(oldMasterAgreementBean.getExoticOptionsSelectedList()).thenReturn(dummyList);
            org.mockito.Mockito.when(oldMasterAgreementBean.getExoticNDOSelectedList()).thenReturn(dummyList);
            
            org.mockito.Mockito.when(maBean.getFxCashDeliverableSelectedList()).thenReturn(dummyList);
            org.mockito.Mockito.when(maBean.getFxOptionsDeliverableSelectedList()).thenReturn(dummyList);
            org.mockito.Mockito.when(maBean.getFxCashNDFSelectedList()).thenReturn(dummyList);
            org.mockito.Mockito.when(maBean.getFxOptionsNDFSelectedList()).thenReturn(dummyList);
            org.mockito.Mockito.when(maBean.getBullionSelectedList()).thenReturn(dummyList);
            org.mockito.Mockito.when(maBean.getBullionCcyPairSelectedList()).thenReturn(dummyList);
            org.mockito.Mockito.when(maBean.getExoticOptionsSelectedList()).thenReturn(dummyList);
            org.mockito.Mockito.when(maBean.getExoticNDOSelectedList()).thenReturn(dummyList);
            
            org.powermock.api.mockito.PowerMockito.doNothing().when(spyTarget, "populateExpandCurrencies2ExpandChanges",
                org.mockito.Mockito.anyInt(),
                org.mockito.Mockito.anyList(),
                org.mockito.Mockito.anyList(),
                org.mockito.Mockito.any(com.traiana.bundle.fxweb.dna.screen.ExpandChanges.class));
                
            org.mockito.Mockito.when(oldMasterAgreementBean.isFxCash()).thenReturn(false);
            org.mockito.Mockito.when(oldMasterAgreementBean.isSpotFarward()).thenReturn(true);
            
            org.mockito.Mockito.when(oldMasterAgreementBean.isNdf()).thenReturn(true);
            org.mockito.Mockito.when(oldMasterAgreementBean.isNdoOptions()).thenReturn(true);
            org.mockito.Mockito.when(oldMasterAgreementBean.isVanillaDeliverableFamily()).thenReturn(true);
            org.mockito.Mockito.when(oldMasterAgreementBean.isExoticOptionFamily()).thenReturn(true);
            org.mockito.Mockito.when(oldMasterAgreementBean.isExoticNdo()).thenReturn(true);
            
            org.mockito.Mockito.when(oldMasterAgreementBean.isBullion()).thenReturn(false);
            org.mockito.Mockito.when(oldMasterAgreementBean.isBullionSpotFarward()).thenReturn(false);
            org.mockito.Mockito.when(oldMasterAgreementBean.isBullionOption()).thenReturn(true);
            
            org.powermock.reflect.Whitebox.invokeMethod(spyTarget, "populateExpandCurrencies2ExpandChanges", oldMasterAgreementBean, expandChanges);
            
            org.mockito.ArgumentCaptor<java.util.Map> mapCaptor = org.mockito.ArgumentCaptor.forClass(java.util.Map.class);
            org.mockito.Mockito.verify(expandChanges).setMastSelectedCurrencyTypeMap(mapCaptor.capture());
            org.junit.Assert.assertEquals(0, mapCaptor.getValue().size());
        }

        @Test
        public void test_populateExpandTenor2ExpandChanges_oldCashNull() throws Exception {
            com.traiana.bundle.setup.dma.ma.MaSaveOp target = new com.traiana.bundle.setup.dma.ma.MaSaveOp();
            com.traiana.bundle.fxweb.dna.screen.ExpandChanges expandChanges = org.mockito.Mockito.mock(com.traiana.bundle.fxweb.dna.screen.ExpandChanges.class);
    
            org.powermock.reflect.Whitebox.invokeMethod(target, "populateExpandTenor2ExpandChanges", 
                "tenor1", (java.lang.String) null, "oldType", "10", "newType", expandChanges);
    
            org.mockito.Mockito.verifyZeroInteractions(expandChanges);
        }

        @Test
        public void test_populateExpandTenor2ExpandChanges_newCashNull() throws Exception {
            com.traiana.bundle.setup.dma.ma.MaSaveOp target = new com.traiana.bundle.setup.dma.ma.MaSaveOp();
            com.traiana.bundle.fxweb.dna.screen.ExpandChanges expandChanges = org.mockito.Mockito.mock(com.traiana.bundle.fxweb.dna.screen.ExpandChanges.class);
    
            org.powermock.reflect.Whitebox.invokeMethod(target, "populateExpandTenor2ExpandChanges", 
                "tenor1", "10", "oldType", (java.lang.String) null, "newType", expandChanges);
    
            org.mockito.Mockito.verifyZeroInteractions(expandChanges);
        }

        @Test
        public void test_populateExpandTenor2ExpandChanges_noExpand() throws Exception {
            com.traiana.bundle.setup.dma.ma.MaSaveOp target = org.powermock.api.mockito.PowerMockito.spy(new com.traiana.bundle.setup.dma.ma.MaSaveOp());
            org.powermock.api.mockito.PowerMockito.mockStatic(com.traiana.bundle.setup.dma.dna.DnaOp.class);
    
            double nYears = (double) com.traiana.bundle.setup.dma.DnmFinals.TENOR_TYPE.N_YEARS;
    
            org.powermock.api.mockito.PowerMockito.doReturn(nYears).when(target, "castToNumberToDouble", "oldType");
            org.powermock.api.mockito.PowerMockito.doReturn(10.0).when(target, "castToNumberToDouble", "10");
            org.powermock.api.mockito.PowerMockito.doReturn(nYears).when(target, "castToNumberToDouble", "newType");
            org.powermock.api.mockito.PowerMockito.doReturn(5.0).when(target, "castToNumberToDouble", "5");
    
            org.powermock.api.mockito.PowerMockito.when(com.traiana.bundle.setup.dma.dna.DnaOp.castToNumberToDouble("newType")).thenReturn(nYears);
            org.powermock.api.mockito.PowerMockito.when(com.traiana.bundle.setup.dma.dna.DnaOp.castToNumberToDouble("5")).thenReturn(5.0);
    
            com.traiana.bundle.fxweb.dna.screen.ExpandChanges expandChanges = org.mockito.Mockito.mock(com.traiana.bundle.fxweb.dna.screen.ExpandChanges.class);
    
            org.powermock.reflect.Whitebox.invokeMethod(target, "populateExpandTenor2ExpandChanges", 
                "tenor1", "10", "oldType", "5", "newType", expandChanges);
    
            org.mockito.Mockito.verifyZeroInteractions(expandChanges);
        }

        @Test
        public void test_populateExpandTenor2ExpandChanges_expand_MonthsToYears() throws Exception {
            com.traiana.bundle.setup.dma.ma.MaSaveOp target = org.powermock.api.mockito.PowerMockito.spy(new com.traiana.bundle.setup.dma.ma.MaSaveOp());
            org.powermock.api.mockito.PowerMockito.mockStatic(com.traiana.bundle.setup.dma.dna.DnaOp.class);
    
            double nYears = (double) com.traiana.bundle.setup.dma.DnmFinals.TENOR_TYPE.N_YEARS;
            double nMonths = (double) com.traiana.bundle.setup.dma.DnmFinals.TENOR_TYPE.N_MONTHS;
            double daysInYear = (double) com.traiana.bundle.setup.dma.DnmFinals.TENOR_TYPE.DAYS_IN_YEAR;
            java.lang.String yearsStr = java.lang.String.valueOf(com.traiana.bundle.setup.dma.DnmFinals.TENOR_TYPE.YEARS);
    
            org.powermock.api.mockito.PowerMockito.doReturn(nMonths).when(target, "castToNumberToDouble", "oldType");
            org.powermock.api.mockito.PowerMockito.doReturn(0.0).when(target, "castToNumberToDouble", "0");
            org.powermock.api.mockito.PowerMockito.doReturn(nYears).when(target, "castToNumberToDouble", "newType");
            org.powermock.api.mockito.PowerMockito.doReturn(10.0).when(target, "castToNumberToDouble", "10");
    
            org.powermock.api.mockito.PowerMockito.when(com.traiana.bundle.setup.dma.dna.DnaOp.castToNumberToDouble("newType")).thenReturn(nYears);
            org.powermock.api.mockito.PowerMockito.when(com.traiana.bundle.setup.dma.dna.DnaOp.castToNumberToDouble("10")).thenReturn(10.0);
    
            com.traiana.bundle.fxweb.dna.screen.ExpandChanges expandChanges = org.mockito.Mockito.mock(com.traiana.bundle.fxweb.dna.screen.ExpandChanges.class);
    
            org.powermock.reflect.Whitebox.invokeMethod(target, "populateExpandTenor2ExpandChanges", 
                "tenor2", "0", "oldType", "10", "newType", expandChanges);
    
            double expectedCulInDays = daysInYear * 10.0;
            java.lang.String expectedDesc = "10 " + yearsStr;
    
            org.mockito.Mockito.verify(expandChanges).addTenor(org.mockito.ArgumentMatchers.eq("tenor2"), 
                org.mockito.ArgumentMatchers.eq(java.lang.Boolean.FALSE), 
                org.mockito.ArgumentMatchers.eq(expectedDesc), 
                org.mockito.ArgumentMatchers.isNull(), 
                org.mockito.ArgumentMatchers.isNull(), 
                org.mockito.ArgumentMatchers.eq(expectedCulInDays));
        }

        @Test
        public void test_populateExpandTenor2ExpandChanges_expand_YearsToMonths() throws Exception {
            com.traiana.bundle.setup.dma.ma.MaSaveOp target = org.powermock.api.mockito.PowerMockito.spy(new com.traiana.bundle.setup.dma.ma.MaSaveOp());
            org.powermock.api.mockito.PowerMockito.mockStatic(com.traiana.bundle.setup.dma.dna.DnaOp.class);
    
            double nYears = (double) com.traiana.bundle.setup.dma.DnmFinals.TENOR_TYPE.N_YEARS;
            double nMonths = (double) com.traiana.bundle.setup.dma.DnmFinals.TENOR_TYPE.N_MONTHS;
            double daysInMonth = (double) com.traiana.bundle.setup.dma.DnmFinals.TENOR_TYPE.DAYS_IN_MONTH;
            java.lang.String monthsStr = java.lang.String.valueOf(com.traiana.bundle.setup.dma.DnmFinals.TENOR_TYPE.MONTHS);
    
            org.powermock.api.mockito.PowerMockito.doReturn(nYears).when(target, "castToNumberToDouble", "oldType");
            org.powermock.api.mockito.PowerMockito.doReturn(0.0).when(target, "castToNumberToDouble", "0");
            org.powermock.api.mockito.PowerMockito.doReturn(nMonths).when(target, "castToNumberToDouble", "newType");
            org.powermock.api.mockito.PowerMockito.doReturn(20.0).when(target, "castToNumberToDouble", "20");
    
            org.powermock.api.mockito.PowerMockito.when(com.traiana.bundle.setup.dma.dna.DnaOp.castToNumberToDouble("newType")).thenReturn(nMonths);
            org.powermock.api.mockito.PowerMockito.when(com.traiana.bundle.setup.dma.dna.DnaOp.castToNumberToDouble("20")).thenReturn(20.0);
    
            com.traiana.bundle.fxweb.dna.screen.ExpandChanges expandChanges = org.mockito.Mockito.mock(com.traiana.bundle.fxweb.dna.screen.ExpandChanges.class);
    
            org.powermock.reflect.Whitebox.invokeMethod(target, "populateExpandTenor2ExpandChanges", 
                "tenor3", "0", "oldType", "20", "newType", expandChanges);
    
            double expectedCulInDays = daysInMonth * 20.0;
            java.lang.String expectedDesc = "20 " + monthsStr;
    
            org.mockito.Mockito.verify(expandChanges).addTenor(org.mockito.ArgumentMatchers.eq("tenor3"), 
                org.mockito.ArgumentMatchers.eq(java.lang.Boolean.FALSE), 
                org.mockito.ArgumentMatchers.eq(expectedDesc), 
                org.mockito.ArgumentMatchers.isNull(), 
                org.mockito.ArgumentMatchers.isNull(), 
                org.mockito.ArgumentMatchers.eq(expectedCulInDays));
        }

        @Test
        public void test_populateExpandTenor2ExpandChanges_expand_DaysToDays() throws Exception {
            com.traiana.bundle.setup.dma.ma.MaSaveOp target = org.powermock.api.mockito.PowerMockito.spy(new com.traiana.bundle.setup.dma.ma.MaSaveOp());
            org.powermock.api.mockito.PowerMockito.mockStatic(com.traiana.bundle.setup.dma.dna.DnaOp.class);
    
            double nYears = (double) com.traiana.bundle.setup.dma.DnmFinals.TENOR_TYPE.N_YEARS;
            double nMonths = (double) com.traiana.bundle.setup.dma.DnmFinals.TENOR_TYPE.N_MONTHS;
            double otherType = nYears + nMonths + 999.0;
            java.lang.String daysStr = java.lang.String.valueOf(com.traiana.bundle.setup.dma.DnmFinals.TENOR_TYPE.DAYS);
    
            org.powermock.api.mockito.PowerMockito.doReturn(otherType).when(target, "castToNumberToDouble", "oldType");
            org.powermock.api.mockito.PowerMockito.doReturn(0.0).when(target, "castToNumberToDouble", "0");
            org.powermock.api.mockito.PowerMockito.doReturn(otherType).when(target, "castToNumberToDouble", "newType");
            org.powermock.api.mockito.PowerMockito.doReturn(30.0).when(target, "castToNumberToDouble", "30");
    
            org.powermock.api.mockito.PowerMockito.when(com.traiana.bundle.setup.dma.dna.DnaOp.castToNumberToDouble("newType")).thenReturn(otherType);
            org.powermock.api.mockito.PowerMockito.when(com.traiana.bundle.setup.dma.dna.DnaOp.castToNumberToDouble("30")).thenReturn(30.0);
    
            com.traiana.bundle.fxweb.dna.screen.ExpandChanges expandChanges = org.mockito.Mockito.mock(com.traiana.bundle.fxweb.dna.screen.ExpandChanges.class);
    
            org.powermock.reflect.Whitebox.invokeMethod(target, "populateExpandTenor2ExpandChanges", 
                "tenor4", "0", "oldType", "30", "newType", expandChanges);
    
            double expectedCulInDays = 1.0 * 30.0;
            java.lang.String expectedDesc = "30 " + daysStr;
    
            org.mockito.Mockito.verify(expandChanges).addTenor(org.mockito.ArgumentMatchers.eq("tenor4"), 
                org.mockito.ArgumentMatchers.eq(java.lang.Boolean.FALSE), 
                org.mockito.ArgumentMatchers.eq(expectedDesc), 
                org.mockito.ArgumentMatchers.isNull(), 
                org.mockito.ArgumentMatchers.isNull(), 
                org.mockito.ArgumentMatchers.eq(expectedCulInDays));
        }

        @Test
        public void test_populateTenorNarrowsSB_BothNull() throws Exception {
            com.traiana.bundle.setup.dma.ma.MaSaveOp target = org.powermock.reflect.Whitebox.newInstance(com.traiana.bundle.setup.dma.ma.MaSaveOp.class);
            com.traiana.bundle.fxweb.dna.screen.NarrowChanges narrowChanges = Mockito.mock(com.traiana.bundle.fxweb.dna.screen.NarrowChanges.class);
            StringBuilder allTenorNarrowSB = new StringBuilder();
    
            org.powermock.reflect.Whitebox.invokeMethod(target, "populateTenorNarrowsSB", 
                "tenorName", (String) null, "oldType", (String) null, "type", narrowChanges, "tenorId", allTenorNarrowSB);
    
            Assert.assertEquals("", allTenorNarrowSB.toString());
            Mockito.verifyZeroInteractions(narrowChanges);
        }

        @Test
        public void test_populateTenorNarrowsSB_CashMaxNull() throws Exception {
            com.traiana.bundle.setup.dma.ma.MaSaveOp target = org.powermock.reflect.Whitebox.newInstance(com.traiana.bundle.setup.dma.ma.MaSaveOp.class);
            com.traiana.bundle.fxweb.dna.screen.NarrowChanges narrowChanges = Mockito.mock(com.traiana.bundle.fxweb.dna.screen.NarrowChanges.class);
            StringBuilder allTenorNarrowSB = new StringBuilder("Initial");
    
            org.powermock.reflect.Whitebox.invokeMethod(target, "populateTenorNarrowsSB", 
                "tenorName", "10", "oldType", (String) null, "type", narrowChanges, "tenorId", allTenorNarrowSB);
    
            Assert.assertEquals("Initial\n     ten