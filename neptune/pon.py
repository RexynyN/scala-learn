import pandas as pd

print(pd.read_csv('Titanic.csv').isnull().sum())