import pandas as pd
import numpy as np
from sklearn.metrics.pairwise import cosine_similarity

ratings_df = pd.read_csv("ratings_musical_filtered.csv")
movies_df = pd.read_csv("movies_musical_filtered.csv")

user_item_matrix = ratings_df.pivot_table(index="movieId", columns="userId", values="rating")

user_item_matrix_filled = user_item_matrix.fillna(0)

item_similarity = cosine_similarity(user_item_matrix_filled)
item_similarity_df = pd.DataFrame(item_similarity, index=user_item_matrix_filled.index, columns=user_item_matrix_filled.index)

def get_recommendations(item_name, top_n=2):
    print(f"Top recommendations for users who liked {item_name}:")
    recommendations = item_similarity_df[item_name].sort_values(ascending=False)
    return recommendations.iloc[1:top_n + 1]

print(get_recommendations(48, top_n=2))