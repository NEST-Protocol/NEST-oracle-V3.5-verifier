package com.nest.ib.helper;

import com.nest.ib.contract.PriceSheet;
import com.nest.ib.contract.PriceSheetPub;
import com.nest.ib.service.BiteService;
import com.nest.ib.utils.EthClient;
import com.nest.ib.config.NestProperties;
import com.nest.ib.constant.Constant;
import com.nest.ib.model.Wallet;
import com.nest.ib.state.Erc20State;
import com.nest.ib.state.VerifyState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.web3j.tuples.generated.Tuple2;

import java.math.BigInteger;
import java.util.*;

/**
 * @author wll
 * @date 2020/8/24 9:47
 */
@Component
public class WalletHelper {

    private static final Logger log = LoggerFactory.getLogger(WalletHelper.class);
    private static Wallet WALLET;

    @Autowired
    private EthClient ethClient;
    @Autowired
    private NestProperties nestProperties;
    @Autowired
    private Erc20State erc20State;
    @Autowired
    private VerifyState verifyState;
    @Autowired
    private BiteService biteService;

    public static void updateWallet(Wallet wallet) {
        WALLET = wallet;
    }

    public static Wallet getWallet() {
        if (WALLET == null) {
            log.warn("The wallet is empty");
            return null;
        }

        return WALLET;
    }

    /**
     * Update wallet assets
     */
    public void updateWalletBalance() {
        if (WALLET != null) {
            if (!biteService.updatePrice()) return;
            updateBalance(WALLET, true);
        }
    }

    /**
     * Update each asset balance：
     *
     * @param sumTotal True counts all assets (account + unfrozen + frozen) and false does not count all assets
     */
    public boolean updateBalance(Wallet wallet, boolean sumTotal) {
        String address = wallet.getCredentials().getAddress();
        // Get the ETH balance of the account
        BigInteger ethBalance = ethClient.ethGetBalance(address);
        if (ethBalance == null) {
            return false;
        }
        // Get the account token balance
        BigInteger tokenBalance = ethClient.ethBalanceOfErc20(address, erc20State.token.getAddress());
        if (tokenBalance == null) {
            return false;
        }
        // Get the NEST balance of your account
        BigInteger nestBalance = ethClient.ethBalanceOfErc20(address, nestProperties.getNestTokenAddress());
        if (nestBalance == null) {
            return false;
        }

        log.info("{} account balance：ETH={}，{}={}，nest={}", address, ethBalance, erc20State.token.getSymbol(), tokenBalance, nestBalance);
        Wallet.Asset account = wallet.getAccount();
        account.setEthAmount(ethBalance);
        account.setNestAmount(nestBalance);
        account.setTokenAmount(tokenBalance);

        // Get the ETH and token balances of CLOSE
        Tuple2<BigInteger, BigInteger> minerEthAndToken = ethClient.getMinerEthAndToken(address, erc20State.token.getAddress());
        if (minerEthAndToken == null) return false;

        BigInteger closeEth = minerEthAndToken.component1();
        BigInteger closeToken = minerEthAndToken.component2();

        Wallet.Asset closed = wallet.getClosed();
        closed.setEthAmount(closeEth);
        closed.setTokenAmount(closeToken);

        // Get the NEST balance of close, which contains the number of NEST mined, and can also be used for direct quotation
        BigInteger closeNest = ethClient.getMinerNest(address);
        if (closeNest == null) return false;
        closed.setNestAmount(closeNest);

        log.info("{} Assets are unfrozen under the contract ETH={}，{}={}，nest={}", address, closeEth, erc20State.token.getSymbol(), closeToken, closeNest);

        // Current Total Available Balance, Account Balance + Unfrozen Balance
        Wallet.Asset useable = wallet.getUseable();
        Wallet.Asset useableTemp = new Wallet.Asset();
        useableTemp.addAsset(closed);
        useableTemp.addAsset(account);
        useable.setAsset(useableTemp);

        // Get the Ntoken balance
        BigInteger nTokenBanlance = null, closeNtoken = null;
        // If it's USDT, NToken is Nest
        if (erc20State.token.getSymbol().equalsIgnoreCase("USDT")) {
            nTokenBanlance = nestBalance;
            closeNtoken = closeNest;
        } else {
            closeNtoken = ethClient.balanceOfTokenInPool(address, erc20State.nToken.getAddress());
            if (closeNtoken == null) return false;

            nTokenBanlance = ethClient.ethBalanceOfErc20(address, erc20State.nToken.getAddress());
            if (nTokenBanlance == null) {
                return false;
            }
        }
        closed.setnTokenAmount(closeNtoken);
        account.setnTokenAmount(nTokenBanlance);

        // Total number of available NTokens
        BigInteger usableNtoken = closeNtoken.add(nTokenBanlance);
        useable.setnTokenAmount(usableNtoken);

        log.info("Total current available  {} balance ：{}", erc20State.nToken.getSymbol(), usableNtoken);

        // Calculate total assets
        if (sumTotal) {
            Wallet.Asset freezedAssetTemp = new Wallet.Asset();

            // Token eating verifies frozen assets
            List<PriceSheet> tokenPriceSheets = ethClient.unClosedSheetListOf(address, erc20State.token.getAddress(), verifyState.getMaxFindNum());
            addFreezedAsset(freezedAssetTemp, tokenPriceSheets, erc20State.token);

            // Ntoken eating verifies frozen assets
            if (verifyState.isMustPost2()) {
                List<PriceSheet> nTokenPriceSheets = ethClient.unClosedSheetListOf(address, erc20State.nToken.getAddress(), verifyState.getMaxFindNum());
                addFreezedAsset(freezedAssetTemp, nTokenPriceSheets, erc20State.nToken);
            }
            wallet.getFreezed().setAsset(freezedAssetTemp);

            Wallet.Asset total = wallet.getTotal();
            Wallet.Asset totalTemp = new Wallet.Asset();
            totalTemp.addAsset(account);
            totalTemp.addAsset(wallet.getFreezed());
            totalTemp.addAsset(closed);
            total.setAsset(totalTemp);
        }
        return true;
    }

    private void addFreezedAsset(Wallet.Asset freezed, List<PriceSheet> priceSheets, Erc20State.Item erc20) {
        if (!CollectionUtils.isEmpty(priceSheets)) {
            for (PriceSheet priceSheet : priceSheets) {
                BigInteger nestNum1k = priceSheet.getNestNum1k();
                BigInteger nestNumBal = nestNum1k.multiply(Constant.BIG_INTEGER_1K);
                // Get the complete quotation information according to the quotation list INDEX, and get the frozen assets
                PriceSheetPub priceSheetPub = ethClient.priceSheet(erc20.getAddress(), priceSheet.index);
                if (priceSheetPub == null) continue;
                // Mortgage of the NEST
                BigInteger nestAmount = nestNumBal.multiply(Constant.UNIT_INT18);
                freezed.addNestAmount(nestAmount);

                // Ethnumbal is the amount of ETH left
                freezed.addEthAmount(priceSheetPub.getEthNumBal().multiply(Constant.UNIT_INT18));

                boolean usdtOffer = erc20State.token.getSymbol().equalsIgnoreCase("USDT");
                // TokenNumbal * TokenAmountPereth is the number of tokens/NTokens left
                if (erc20.getAddress().equalsIgnoreCase(erc20State.token.getAddress())) {
                    freezed.addTokenAmount(priceSheetPub.getTokenNumBal().multiply(priceSheet.tokenAmountPerEth));
                    if (usdtOffer) {
                        freezed.addNtokenAmount(nestAmount);
                    }
                } else {
                    if (usdtOffer) {
                        // USDT quotation, frozen Ntoken is NEST, need to add the NEST of mortgage
                        BigInteger freezedNtoken = priceSheetPub.getTokenNumBal().multiply(priceSheet.tokenAmountPerEth);
                        freezed.addNtokenAmount(freezedNtoken);
                        freezed.addNtokenAmount(nestAmount);
                        freezed.addNestAmount(freezedNtoken);
                    } else {
                        freezed.addNtokenAmount(priceSheetPub.getTokenNumBal().multiply(priceSheet.tokenAmountPerEth));
                    }
                }

            }
        }
    }
}
